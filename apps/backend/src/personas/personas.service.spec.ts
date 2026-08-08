import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { AiService } from '../ai/ai.service';
import { HabitData, HabitRepository } from '../habits/habit.repository';
import { DriftFlagRepository } from '../notifications/drift-flag.repository';
import { FIREBASE_MESSAGING } from '../notifications/firebase.module';
import { UserData, UserRepository } from '../users/user.repository';
import { PersonasService } from './personas.service';

const mockUserRepository = {
  findUserById: jest.fn(),
};

const mockHabitRepository = {
  findByUserId: jest.fn(),
};

const mockAiService = {
  detectDrift: jest.fn(),
  getFeedbackTally: jest.fn(),
};

const mockDriftFlagRepository = {
  create: jest.fn(),
};

const mockMessaging = {
  send: jest.fn(),
};

const USER_ID = 'user-123';

const makeUser = (overrides: Partial<UserData> = {}): UserData => ({
  id: USER_ID,
  email: 'user@example.com',
  password: 'hashed',
  goal: 'Stay consistent',
  personaType: 'Achiever',
  motivationalMessage: 'Keep going',
  coreGoals: [],
  dailyVariations: [],
  tasksLastGeneratedDate: '',
  ...overrides,
});

const makeHabit = (overrides: Partial<HabitData> = {}): HabitData => ({
  id: 'habit-1',
  userId: USER_ID,
  title: 'Morning Run',
  description: '',
  frequency: 'daily',
  targetCount: 1,
  streak: 0,
  completionHistory: [],
  persona: '',
  isArchived: false,
  consistencyScore: 0,
  completionNotes: [],
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('PersonasService', () => {
  let service: PersonasService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PersonasService,
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: HabitRepository, useValue: mockHabitRepository },
        { provide: AiService, useValue: mockAiService },
        { provide: DriftFlagRepository, useValue: mockDriftFlagRepository },
        { provide: FIREBASE_MESSAGING, useValue: mockMessaging },
      ],
    }).compile();

    service = module.get<PersonasService>(PersonasService);
    jest.clearAllMocks();
    mockAiService.getFeedbackTally.mockReturnValue({ positiveFeedbackCount: 0, negativeFeedbackCount: 0 });
  });

  describe('driftCheck()', () => {
    it('throws NotFoundException when user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.driftCheck(USER_ID)).rejects.toThrow(NotFoundException);
    });

    it('passes the highest habit streak as activeStreak', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', streak: 3 }),
        makeHabit({ id: 'h2', streak: 7 }),
      ]);
      mockAiService.detectDrift.mockResolvedValue({});

      await service.driftCheck(USER_ID);

      expect(mockAiService.detectDrift).toHaveBeenCalledWith(
        expect.objectContaining({ behaviorSnapshot: expect.objectContaining({ activeStreak: 7 }) }),
      );
    });

    it('lets an explicit activeStreak override the derived habit streak', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit({ streak: 7 })]);
      mockAiService.detectDrift.mockResolvedValue({});

      await service.driftCheck(USER_ID, { activeStreak: 2 });

      expect(mockAiService.detectDrift).toHaveBeenCalledWith(
        expect.objectContaining({ behaviorSnapshot: expect.objectContaining({ activeStreak: 2 }) }),
      );
    });
  });
});
