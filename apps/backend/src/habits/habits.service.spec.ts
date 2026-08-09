import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { CreateHabitDto } from './dto/create-habit.dto';
import { HabitData, HabitRepository } from './habit.repository';
import { GoalData, GoalRepository } from '../goals/goal.repository';
import { AiService } from '../ai/ai.service';
import { HabitsService } from './habits.service';

const mockHabitRepository = {
  createHabit: jest.fn(),
  findByUserId: jest.fn(),
  findById: jest.fn(),
  updateHabit: jest.fn(),
  deleteHabit: jest.fn(),
  completeHabit: jest.fn(),
};

const mockGoalRepository = {
  findById: jest.fn(),
};

const mockAiService = {
  checkHabitGoalRelevance: jest.fn(),
  checkTaskVerification: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-999';
const HABIT_ID = 'habit-abc';
const GOAL_ID = 'goal-abc';

const makeHabit = (overrides: Partial<HabitData> = {}): HabitData => ({
  id: HABIT_ID,
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
  targetDate: new Date('2026-12-31').toISOString(),
  status: 'active',
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('HabitsService', () => {
  let service: HabitsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        HabitsService,
        { provide: HabitRepository, useValue: mockHabitRepository },
        { provide: GoalRepository, useValue: mockGoalRepository },
        { provide: AiService, useValue: mockAiService },
      ],
    }).compile();

    service = module.get<HabitsService>(HabitsService);
    jest.clearAllMocks();
    mockAiService.checkHabitGoalRelevance.mockResolvedValue({ isRelated: true, reason: '' });
    mockAiService.checkTaskVerification.mockResolvedValue({ isPlausible: true, reason: '' });
  });

  describe('createHabit()', () => {
    it('injects userId from the caller — never from the DTO', async () => {
      const dto: CreateHabitDto = { title: 'Morning Run', frequency: 'daily' };
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      const expected = makeHabit();
      mockHabitRepository.createHabit.mockResolvedValue(expected);

      const result = await service.createHabit(USER_ID, dto);

      expect(mockHabitRepository.createHabit).toHaveBeenCalledWith(
        expect.objectContaining({ userId: USER_ID, title: 'Morning Run', frequency: 'daily' }),
      );
      expect(result).toEqual(expected);
    });

    it('rejects a 3rd standalone habit', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1' }),
        makeHabit({ id: 'h2' }),
      ]);

      await expect(
        service.createHabit(USER_ID, { title: 'Read', frequency: 'daily' }),
      ).rejects.toThrow(BadRequestException);
      expect(mockHabitRepository.createHabit).not.toHaveBeenCalled();
    });

    it('treats pre-existing habits with no goalId as standalone when counting the cap', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', goalId: undefined }),
        makeHabit({ id: 'h2', goalId: undefined }),
      ]);

      await expect(
        service.createHabit(USER_ID, { title: 'Read', frequency: 'daily' }),
      ).rejects.toThrow(BadRequestException);
    });

    it('rejects linking to a goal that does not exist', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockGoalRepository.findById.mockResolvedValue(null);

      await expect(
        service.createHabit(USER_ID, { title: 'Long run', frequency: 'weekly', goalId: GOAL_ID }),
      ).rejects.toThrow(BadRequestException);
      expect(mockHabitRepository.createHabit).not.toHaveBeenCalled();
    });

    it('rejects linking to another user\'s goal', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ userId: OTHER_USER_ID }));

      await expect(
        service.createHabit(USER_ID, { title: 'Long run', frequency: 'weekly', goalId: GOAL_ID }),
      ).rejects.toThrow(ForbiddenException);
    });

    it('rejects linking to a goal that is not active', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ status: 'forfeited' }));

      await expect(
        service.createHabit(USER_ID, { title: 'Long run', frequency: 'weekly', goalId: GOAL_ID }),
      ).rejects.toThrow(BadRequestException);
    });

    it('rejects a 4th habit linked to the same goal', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', goalId: GOAL_ID }),
        makeHabit({ id: 'h2', goalId: GOAL_ID }),
        makeHabit({ id: 'h3', goalId: GOAL_ID }),
      ]);

      await expect(
        service.createHabit(USER_ID, { title: 'Long run', frequency: 'weekly', goalId: GOAL_ID }),
      ).rejects.toThrow(BadRequestException);
      expect(mockHabitRepository.createHabit).not.toHaveBeenCalled();
    });

    it('allows linking to a freshly created goal, independent of a prior (no longer active) goal\'s habit count', async () => {
      const newGoalId = 'goal-new';
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ id: newGoalId }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1', goalId: GOAL_ID }),
        makeHabit({ id: 'h2', goalId: GOAL_ID }),
        makeHabit({ id: 'h3', goalId: GOAL_ID }),
      ]);
      const created = makeHabit({ id: 'h4', goalId: newGoalId });
      mockHabitRepository.createHabit.mockResolvedValue(created);

      const result = await service.createHabit(USER_ID, {
        title: 'Long run',
        frequency: 'weekly',
        goalId: newGoalId,
      });

      expect(result).toEqual(created);
    });

    it('surfaces a relevanceWarning without blocking creation when the AI flags a mismatch', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockAiService.checkHabitGoalRelevance.mockResolvedValue({
        isRelated: false,
        reason: 'Watching TV does not support running a marathon.',
      });
      const created = makeHabit({ goalId: GOAL_ID });
      mockHabitRepository.createHabit.mockResolvedValue(created);

      const result = await service.createHabit(USER_ID, {
        title: 'Watch Netflix',
        frequency: 'daily',
        goalId: GOAL_ID,
      });

      expect(mockHabitRepository.createHabit).toHaveBeenCalled();
      expect(result.relevanceWarning).toBe('Watching TV does not support running a marathon.');
    });

    it('does not skip AI relevance checking for standalone habits (no goal to check against)', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([]);

      await service.createHabit(USER_ID, { title: 'Read', frequency: 'daily' });

      expect(mockAiService.checkHabitGoalRelevance).not.toHaveBeenCalled();
    });
  });

  describe('updateHabit()', () => {
    it('throws NotFoundException when the habit does not exist', async () => {
      mockHabitRepository.findById.mockResolvedValue(null);

      await expect(service.updateHabit(USER_ID, HABIT_ID, { title: 'New title' })).rejects.toThrow(
        NotFoundException,
      );
    });

    it('throws ForbiddenException when the caller does not own the habit', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ userId: OTHER_USER_ID }));

      await expect(service.updateHabit(USER_ID, HABIT_ID, { title: 'New title' })).rejects.toThrow(
        ForbiddenException,
      );
    });

    it('leaves the existing goal link untouched when goalId is omitted', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ goalId: GOAL_ID }));
      mockHabitRepository.updateHabit.mockResolvedValue(makeHabit({ goalId: GOAL_ID, title: 'New title' }));

      await service.updateHabit(USER_ID, HABIT_ID, { title: 'New title' });

      expect(mockGoalRepository.findById).not.toHaveBeenCalled();
      expect(mockHabitRepository.updateHabit).toHaveBeenCalledWith(
        HABIT_ID,
        expect.objectContaining({ goalId: undefined }),
      );
    });

    it('unlinks to standalone when goalId is explicitly null', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ goalId: GOAL_ID }));
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit({ goalId: GOAL_ID })]);
      mockHabitRepository.updateHabit.mockResolvedValue(makeHabit({ goalId: undefined }));

      await service.updateHabit(USER_ID, HABIT_ID, { goalId: null });

      expect(mockHabitRepository.updateHabit).toHaveBeenCalledWith(HABIT_ID, expect.objectContaining({ goalId: null }));
    });

    it('rejects unlinking to standalone when the standalone cap is already full', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ goalId: GOAL_ID }));
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: 'h1' }),
        makeHabit({ id: 'h2' }),
      ]);

      await expect(service.updateHabit(USER_ID, HABIT_ID, { goalId: null })).rejects.toThrow(BadRequestException);
      expect(mockHabitRepository.updateHabit).not.toHaveBeenCalled();
    });

    it('excludes the habit being updated from its own cap count when relinking', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ goalId: GOAL_ID }));
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ id: HABIT_ID, goalId: GOAL_ID }),
        makeHabit({ id: 'h2', goalId: GOAL_ID }),
        makeHabit({ id: 'h3', goalId: GOAL_ID }),
      ]);
      mockHabitRepository.updateHabit.mockResolvedValue(makeHabit({ goalId: GOAL_ID }));

      await service.updateHabit(USER_ID, HABIT_ID, { goalId: GOAL_ID });

      expect(mockHabitRepository.updateHabit).toHaveBeenCalledWith(HABIT_ID, expect.objectContaining({ goalId: GOAL_ID }));
    });

    it('rejects relinking to a goal the user does not own', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit());
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ userId: OTHER_USER_ID }));

      await expect(service.updateHabit(USER_ID, HABIT_ID, { goalId: GOAL_ID })).rejects.toThrow(
        ForbiddenException,
      );
    });

    it('surfaces a relevanceWarning on relink without blocking the update', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ title: 'Watch Netflix' }));
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      mockHabitRepository.findByUserId.mockResolvedValue([]);
      mockAiService.checkHabitGoalRelevance.mockResolvedValue({ isRelated: false, reason: 'Not related.' });
      mockHabitRepository.updateHabit.mockResolvedValue(makeHabit({ title: 'Watch Netflix', goalId: GOAL_ID }));

      const result = await service.updateHabit(USER_ID, HABIT_ID, { goalId: GOAL_ID });

      expect(result.relevanceWarning).toBe('Not related.');
      expect(mockHabitRepository.updateHabit).toHaveBeenCalled();
    });
  });

  describe('findByUserId()', () => {
    it('returns non-archived habits for the user', async () => {
      const habits = [
        makeHabit({ id: 'h1', title: 'Run' }),
        makeHabit({ id: 'h2', title: 'Read' }),
      ];
      mockHabitRepository.findByUserId.mockResolvedValue(habits);

      const result = await service.findByUserId(USER_ID);

      expect(mockHabitRepository.findByUserId).toHaveBeenCalledWith(USER_ID);
      expect(result).toHaveLength(2);
      expect(result.every((h) => !h.isArchived)).toBe(true);
    });
  });

  describe('deleteHabit()', () => {
    it('throws NotFoundException when habit does not exist', async () => {
      mockHabitRepository.findById.mockResolvedValue(null);

      await expect(service.deleteHabit(USER_ID, HABIT_ID)).rejects.toThrow(NotFoundException);
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
    });

    it('throws ForbiddenException and skips repository when caller does not own the habit', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ userId: OTHER_USER_ID }));

      await expect(service.deleteHabit(USER_ID, HABIT_ID)).rejects.toThrow(ForbiddenException);
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
    });
  });

  describe('completeHabit()', () => {
    it('throws ForbiddenException and skips repository when caller does not own the habit', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ userId: OTHER_USER_ID }));

      await expect(service.completeHabit(USER_ID, HABIT_ID)).rejects.toThrow(ForbiddenException);
      expect(mockHabitRepository.completeHabit).not.toHaveBeenCalled();
    });

    it('returns streak incremented by 1 on a consecutive day completion', async () => {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const yesterdayStr = yesterday.toISOString().split('T')[0];
      const todayStr = new Date().toISOString().split('T')[0];

      mockHabitRepository.findById.mockResolvedValue(
        makeHabit({ streak: 1, completionHistory: [yesterdayStr] }),
      );
      mockHabitRepository.completeHabit.mockResolvedValue(
        makeHabit({ streak: 2, completionHistory: [yesterdayStr, todayStr] }),
      );

      const result = await service.completeHabit(USER_ID, HABIT_ID);

      expect(mockHabitRepository.completeHabit).toHaveBeenCalledWith(HABIT_ID, undefined);
      expect(result.streak).toBe(2);
    });

    it('resets streak to 1 when a day was missed before today', async () => {
      const twoDaysAgo = new Date();
      twoDaysAgo.setDate(twoDaysAgo.getDate() - 2);
      const twoDaysAgoStr = twoDaysAgo.toISOString().split('T')[0];
      const todayStr = new Date().toISOString().split('T')[0];

      mockHabitRepository.findById.mockResolvedValue(
        makeHabit({ streak: 3, completionHistory: [twoDaysAgoStr] }),
      );
      mockHabitRepository.completeHabit.mockResolvedValue(
        makeHabit({ streak: 1, completionHistory: [twoDaysAgoStr, todayStr] }),
      );

      const result = await service.completeHabit(USER_ID, HABIT_ID);

      expect(result.streak).toBe(1);
    });

    it('does not call the AI verification check when no note is provided', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit());
      mockHabitRepository.completeHabit.mockResolvedValue(makeHabit());

      await service.completeHabit(USER_ID, HABIT_ID);

      expect(mockAiService.checkTaskVerification).not.toHaveBeenCalled();
    });

    it('passes the note through to the repository and checks it against the habit title', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ title: 'Morning Run' }));
      mockHabitRepository.completeHabit.mockResolvedValue(makeHabit());

      await service.completeHabit(USER_ID, HABIT_ID, 'Ran 5km this morning');

      expect(mockHabitRepository.completeHabit).toHaveBeenCalledWith(HABIT_ID, 'Ran 5km this morning');
      expect(mockAiService.checkTaskVerification).toHaveBeenCalledWith({
        habitTitle: 'Morning Run',
        note: 'Ran 5km this morning',
      });
    });

    it('surfaces a verificationWarning without blocking completion when the AI flags a mismatch', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit());
      mockHabitRepository.completeHabit.mockResolvedValue(makeHabit({ streak: 1 }));
      mockAiService.checkTaskVerification.mockResolvedValue({
        isPlausible: false,
        reason: 'Watching TV is not a run.',
      });

      const result = await service.completeHabit(USER_ID, HABIT_ID, 'Watched TV all day');

      expect(result.streak).toBe(1);
      expect(result.verificationWarning).toBe('Watching TV is not a run.');
    });

    it('omits verificationWarning when the note is plausible', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit());
      mockHabitRepository.completeHabit.mockResolvedValue(makeHabit());

      const result = await service.completeHabit(USER_ID, HABIT_ID, 'Ran 5km this morning');

      expect(result.verificationWarning).toBeUndefined();
    });
  });

  describe('getStats()', () => {
    it('throws NotFoundException when the habit does not exist', async () => {
      mockHabitRepository.findById.mockResolvedValue(null);

      await expect(service.getStats(USER_ID, HABIT_ID)).rejects.toThrow(NotFoundException);
    });

    it('throws ForbiddenException when the caller does not own the habit', async () => {
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ userId: OTHER_USER_ID }));

      await expect(service.getStats(USER_ID, HABIT_ID)).rejects.toThrow(ForbiddenException);
    });

    it('returns correct completion aggregates for a habit completed every day since creation', async () => {
      const createdAt = new Date();
      createdAt.setUTCDate(createdAt.getUTCDate() - 3);

      mockHabitRepository.findById.mockResolvedValue(
        makeHabit({
          createdAt: createdAt.toISOString(),
          completionHistory: ['d1', 'd2', 'd3', 'd4'],
          streak: 4,
          consistencyScore: 0.8,
          implementedAt: undefined,
        }),
      );

      const result = await service.getStats(USER_ID, HABIT_ID);

      expect(result).toEqual({
        habitId: HABIT_ID,
        totalCompletions: 4,
        currentStreak: 4,
        consistencyScore: 0.8,
        daysSinceCreation: 4,
        completionRate: 1,
        implementedAt: undefined,
      });
    });

    it('surfaces implementedAt when the habit has crossed the threshold', async () => {
      const implementedAt = new Date().toISOString();
      mockHabitRepository.findById.mockResolvedValue(makeHabit({ implementedAt }));

      const result = await service.getStats(USER_ID, HABIT_ID);

      expect(result.implementedAt).toBe(implementedAt);
    });
  });
});
