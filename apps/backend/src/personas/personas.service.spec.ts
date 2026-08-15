import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { AiService } from '../ai/ai.service';
import { HabitData, HabitRepository } from '../habits/habit.repository';
import { GoalData, GoalRepository } from '../goals/goal.repository';
import { GoalsService } from '../goals/goals.service';
import { DriftFlagRepository } from '../notifications/drift-flag.repository';
import { FIREBASE_MESSAGING } from '../notifications/firebase.module';
import { UserData, UserRepository } from '../users/user.repository';
import { PersonasService } from './personas.service';

const mockUserRepository = {
  findUserById: jest.fn(),
  updateUserPersona: jest.fn(),
  updateUserDailyTasks: jest.fn(),
};

const mockHabitRepository = {
  findByUserId: jest.fn(),
};

const mockGoalRepository = {
  findActiveByUserId: jest.fn(),
};

const mockGoalsService = {
  forfeitGoal: jest.fn(),
};

const mockAiService = {
  detectDrift: jest.fn(),
  getFeedbackTally: jest.fn(),
  generateDailyVariations: jest.fn(),
};

const mockDriftFlagRepository = {
  create: jest.fn(),
};

const mockMessaging = {
  send: jest.fn(),
};

const USER_ID = 'user-123';
const GOAL_ID = 'goal-123';

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

const makeGoal = (overrides: Partial<GoalData> = {}): GoalData => ({
  id: GOAL_ID,
  userId: USER_ID,
  title: 'Run a marathon',
  targetDate: '2026-12-31',
  status: 'active',
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
        { provide: GoalRepository, useValue: mockGoalRepository },
        { provide: GoalsService, useValue: mockGoalsService },
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

  describe('confirmCoachChange()', () => {
    it('personaSwitch: updates the user persona directly', async () => {
      mockUserRepository.updateUserPersona.mockResolvedValue(makeUser({ personaType: 'Grower' }));

      const result = await service.confirmCoachChange(USER_ID, {
        type: 'personaSwitch',
        suggestedPersona: 'Grower',
      });

      expect(mockUserRepository.updateUserPersona).toHaveBeenCalledWith(USER_ID, { personaType: 'Grower' });
      expect(result).toEqual({ type: 'personaSwitch', personaType: 'Grower' });
    });

    it('adjustDifficulty: regenerates daily variations with the bias and persists them', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockAiService.generateDailyVariations.mockResolvedValue([{ description: 'Harder run', points: 40 }]);
      mockUserRepository.updateUserDailyTasks.mockResolvedValue(makeUser());

      const result = await service.confirmCoachChange(USER_ID, { type: 'adjustDifficulty', direction: 'increase' });

      expect(mockAiService.generateDailyVariations).toHaveBeenCalledWith(
        expect.objectContaining({ id: USER_ID }),
        expect.any(Number),
        'increase',
      );
      expect(result.type).toBe('adjustDifficulty');
      if (result.type === 'adjustDifficulty') {
        expect(result.dailyVariations).toEqual([
          expect.objectContaining({ description: 'Harder run', points: 40, completed: false }),
        ]);
      }
    });

    it('forfeitGoal: delegates to GoalsService.forfeitGoal', async () => {
      const forfeitedGoal = makeGoal({ status: 'forfeited' });
      mockGoalsService.forfeitGoal.mockResolvedValue(forfeitedGoal);

      const result = await service.confirmCoachChange(USER_ID, { type: 'forfeitGoal', goalId: GOAL_ID });

      expect(mockGoalsService.forfeitGoal).toHaveBeenCalledWith(USER_ID, GOAL_ID);
      expect(result).toEqual({ type: 'forfeitGoal', goal: forfeitedGoal });
    });
  });
});
