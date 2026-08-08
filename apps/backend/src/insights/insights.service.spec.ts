import { BadRequestException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { AiService } from '../ai/ai.service';
import { HabitData, HabitRepository } from '../habits/habit.repository';
import { UserData, UserRepository } from '../users/user.repository';
import { InsightsService } from './insights.service';

const mockUserRepository = {
  findUserById: jest.fn(),
};

const mockHabitRepository = {
  findByUserId: jest.fn(),
};

const mockAiService = {
  getWeeklyInsights: jest.fn(),
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
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('InsightsService', () => {
  let service: InsightsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        InsightsService,
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: HabitRepository, useValue: mockHabitRepository },
        { provide: AiService, useValue: mockAiService },
      ],
    }).compile();

    service = module.get<InsightsService>(InsightsService);
    jest.clearAllMocks();
  });

  describe('getWeeklyInsights()', () => {
    it('throws NotFoundException when user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.getWeeklyInsights(USER_ID)).rejects.toThrow(NotFoundException);
    });

    it('throws BadRequestException when persona is unknown', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser({ personaType: 'Unknown' }));

      await expect(service.getWeeklyInsights(USER_ID)).rejects.toThrow(BadRequestException);
    });

    it('passes the highest habit streak as currentStreak', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', streak: 2 }),
        makeHabit({ id: 'h2', streak: 5 }),
      ]);
      mockAiService.getWeeklyInsights.mockResolvedValue({});

      await service.getWeeklyInsights(USER_ID);

      expect(mockAiService.getWeeklyInsights).toHaveBeenCalledWith(
        expect.objectContaining({ currentStreak: 5 }),
      );
    });

    it('defaults currentStreak to 0 when the user has no habits', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockAiService.getWeeklyInsights.mockResolvedValue({});

      await service.getWeeklyInsights(USER_ID);

      expect(mockAiService.getWeeklyInsights).toHaveBeenCalledWith(
        expect.objectContaining({ currentStreak: 0 }),
      );
    });
  });
});
