import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { GoalData, GoalRepository } from './goal.repository';
import { UserRepository } from '../users/user.repository';
import { HabitRepository } from '../habits/habit.repository';
import { AiService } from '../ai/ai.service';
import { GoalsService, GOAL_ACHIEVEMENT_MEDAL } from './goals.service';

const mockGoalRepository = {
  createGoal: jest.fn(),
  findOrCreateActiveGoal: jest.fn(),
  findActiveByUserId: jest.fn(),
  findById: jest.fn(),
  updateStatus: jest.fn(),
};

const mockUserRepository = {
  addAchievement: jest.fn(),
  findUserById: jest.fn(),
};

const mockHabitRepository = {
  findByUserId: jest.fn(),
  updateHabit: jest.fn(),
  deleteHabit: jest.fn(),
};

const mockAiService = {
  checkGoalRelevance: jest.fn(),
};

const USER_ID = 'user-123';
const OTHER_USER_ID = 'user-456';
const GOAL_ID = 'goal-abc';

const makeGoal = (overrides: Partial<GoalData> = {}): GoalData => ({
  id: GOAL_ID,
  userId: USER_ID,
  title: 'Run a marathon',
  targetDate: new Date('2026-12-31').toISOString(),
  status: 'active',
  createdAt: new Date().toISOString(),
  ...overrides,
});

describe('GoalsService', () => {
  let service: GoalsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        GoalsService,
        { provide: GoalRepository, useValue: mockGoalRepository },
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: HabitRepository, useValue: mockHabitRepository },
        { provide: AiService, useValue: mockAiService },
      ],
    }).compile();

    service = module.get<GoalsService>(GoalsService);
    jest.clearAllMocks();
  });

  describe('createGoal()', () => {
    it('rejects a second active goal for the same user', async () => {
      mockGoalRepository.findActiveByUserId.mockResolvedValue(makeGoal());

      await expect(
        service.createGoal(USER_ID, { title: 'Learn guitar', targetDate: '2026-12-31' }),
      ).rejects.toThrow(BadRequestException);
      expect(mockGoalRepository.createGoal).not.toHaveBeenCalled();
    });

    it('creates a goal when the user has no active goal', async () => {
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      const created = makeGoal();
      mockGoalRepository.createGoal.mockResolvedValue(created);

      const result = await service.createGoal(USER_ID, { title: 'Run a marathon', targetDate: '2026-12-31' });

      expect(mockGoalRepository.createGoal).toHaveBeenCalledWith({
        userId: USER_ID,
        title: 'Run a marathon',
        targetDate: new Date('2026-12-31'),
      });
      expect(result).toEqual(created);
    });
  });

  describe('getActiveGoal()', () => {
    it('returns null when the user has no active goal and no onboarding goal string', async () => {
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockUserRepository.findUserById.mockResolvedValue({ id: USER_ID, goal: null });

      const result = await service.getActiveGoal(USER_ID);

      expect(result).toBeNull();
      expect(mockGoalRepository.createGoal).not.toHaveBeenCalled();
    });

    it('returns the active goal when one exists', async () => {
      const goal = makeGoal();
      mockGoalRepository.findActiveByUserId.mockResolvedValue(goal);

      const result = await service.getActiveGoal(USER_ID);

      expect(result).toEqual(goal);
      expect(mockUserRepository.findUserById).not.toHaveBeenCalled();
    });

    it('auto-creates a Goal from the legacy onboarding goal string when no active goal exists', async () => {
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockUserRepository.findUserById.mockResolvedValue({ id: USER_ID, goal: 'Run a marathon' });
      const created = makeGoal();
      mockGoalRepository.findOrCreateActiveGoal.mockResolvedValue(created);

      const result = await service.getActiveGoal(USER_ID);

      expect(mockGoalRepository.findOrCreateActiveGoal).toHaveBeenCalledWith({
        userId: USER_ID,
        title: 'Run a marathon',
        targetDate: expect.any(Date),
      });
      expect(result).toEqual(created);
    });

    it('is race-safe: concurrent auto-creates for the same user resolve to the same goal via the atomic upsert', async () => {
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockUserRepository.findUserById.mockResolvedValue({ id: USER_ID, goal: 'Run a marathon' });
      const winner = makeGoal();
      mockGoalRepository.findOrCreateActiveGoal.mockResolvedValue(winner);

      const [a, b] = await Promise.all([service.getActiveGoal(USER_ID), service.getActiveGoal(USER_ID)]);

      expect(a).toEqual(winner);
      expect(b).toEqual(winner);
      expect(mockGoalRepository.createGoal).not.toHaveBeenCalled();
    });
  });

  describe('createGoal() race safety', () => {
    it('converts a duplicate-key error (E11000) from a raced concurrent create into the same friendly rejection', async () => {
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockGoalRepository.createGoal.mockRejectedValue(Object.assign(new Error('duplicate key'), { code: 11000 }));

      await expect(
        service.createGoal(USER_ID, { title: 'Learn guitar', targetDate: '2026-12-31' }),
      ).rejects.toThrow(BadRequestException);
    });
  });

  describe('forfeitGoal()', () => {
    it('throws NotFoundException when the goal does not exist', async () => {
      mockGoalRepository.findById.mockResolvedValue(null);

      await expect(service.forfeitGoal(USER_ID, GOAL_ID)).rejects.toThrow(NotFoundException);
    });

    it('throws ForbiddenException when the caller does not own the goal', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ userId: OTHER_USER_ID }));

      await expect(service.forfeitGoal(USER_ID, GOAL_ID)).rejects.toThrow(ForbiddenException);
      expect(mockGoalRepository.updateStatus).not.toHaveBeenCalled();
    });

    it('throws BadRequestException when the goal is not active', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ status: 'achieved' }));

      await expect(service.forfeitGoal(USER_ID, GOAL_ID)).rejects.toThrow(BadRequestException);
      expect(mockGoalRepository.updateStatus).not.toHaveBeenCalled();
    });

    it('transitions an active goal to forfeited', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      const forfeited = makeGoal({ status: 'forfeited' });
      mockGoalRepository.updateStatus.mockResolvedValue(forfeited);

      const result = await service.forfeitGoal(USER_ID, GOAL_ID);

      expect(mockGoalRepository.updateStatus).toHaveBeenCalledWith(GOAL_ID, 'forfeited');
      expect(result).toEqual(forfeited);
    });
  });

  describe('achieveGoal()', () => {
    it('throws NotFoundException when the goal does not exist', async () => {
      mockGoalRepository.findById.mockResolvedValue(null);

      await expect(service.achieveGoal(USER_ID, GOAL_ID)).rejects.toThrow(NotFoundException);
      expect(mockUserRepository.addAchievement).not.toHaveBeenCalled();
    });

    it('throws ForbiddenException when the caller does not own the goal', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ userId: OTHER_USER_ID }));

      await expect(service.achieveGoal(USER_ID, GOAL_ID)).rejects.toThrow(ForbiddenException);
      expect(mockGoalRepository.updateStatus).not.toHaveBeenCalled();
      expect(mockUserRepository.addAchievement).not.toHaveBeenCalled();
    });

    it('throws BadRequestException when the goal is not active', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ status: 'forfeited' }));

      await expect(service.achieveGoal(USER_ID, GOAL_ID)).rejects.toThrow(BadRequestException);
      expect(mockGoalRepository.updateStatus).not.toHaveBeenCalled();
      expect(mockUserRepository.addAchievement).not.toHaveBeenCalled();
    });

    it('transitions an active goal to achieved and awards a medal exactly once', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      const achieved = makeGoal({ status: 'achieved' });
      mockGoalRepository.updateStatus.mockResolvedValue(achieved);

      const result = await service.achieveGoal(USER_ID, GOAL_ID);

      expect(mockGoalRepository.updateStatus).toHaveBeenCalledWith(GOAL_ID, 'achieved');
      expect(mockUserRepository.addAchievement).toHaveBeenCalledTimes(1);
      expect(mockUserRepository.addAchievement).toHaveBeenCalledWith(
        USER_ID,
        expect.objectContaining({ goalId: GOAL_ID, medal: GOAL_ACHIEVEMENT_MEDAL }),
      );
      expect(result).toEqual(achieved);
    });

    it('does not award a second medal on a re-fetch/re-call once already achieved', async () => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal({ status: 'achieved' }));

      await expect(service.achieveGoal(USER_ID, GOAL_ID)).rejects.toThrow(BadRequestException);
      expect(mockUserRepository.addAchievement).not.toHaveBeenCalled();
    });
  });

  describe('transitionGoal()', () => {
    const NEW_GOAL_ID = 'goal-new';
    const dto = { resolution: 'achieve' as const, newGoalTitle: 'Run 20km', newGoalTargetDate: '2027-06-30' };

    beforeEach(() => {
      mockGoalRepository.findById.mockResolvedValue(makeGoal());
      mockGoalRepository.updateStatus.mockResolvedValue(makeGoal({ status: 'achieved' }));
      mockGoalRepository.findActiveByUserId.mockResolvedValue(null);
      mockGoalRepository.createGoal.mockResolvedValue(makeGoal({ id: NEW_GOAL_ID, title: dto.newGoalTitle }));
      mockUserRepository.addAchievement.mockResolvedValue(undefined);
    });

    it('resolves the old goal and creates the new one, without touching any habits', async () => {
      const result = await service.transitionGoal(USER_ID, GOAL_ID, dto);

      expect(mockGoalRepository.updateStatus).toHaveBeenCalledWith(GOAL_ID, 'achieved');
      expect(mockGoalRepository.createGoal).toHaveBeenCalledWith({
        userId: USER_ID,
        title: dto.newGoalTitle,
        targetDate: new Date(dto.newGoalTargetDate),
      });
      expect(result.newGoal.id).toEqual(NEW_GOAL_ID);
      expect(mockAiService.checkGoalRelevance).not.toHaveBeenCalled();
      expect(mockHabitRepository.findByUserId).not.toHaveBeenCalled();
    });

    it('forfeits the old goal instead of achieving it when resolution is forfeit', async () => {
      await service.transitionGoal(USER_ID, GOAL_ID, { ...dto, resolution: 'forfeit' });

      expect(mockGoalRepository.updateStatus).toHaveBeenCalledWith(GOAL_ID, 'forfeited');
      expect(mockUserRepository.addAchievement).not.toHaveBeenCalled();
    });
  });

  describe('resolveHabits()', () => {
    const NEW_GOAL_ID = 'goal-new';

    const makeHabit = (overrides: Record<string, unknown> = {}) => ({
      id: 'habit-1',
      userId: USER_ID,
      goalId: GOAL_ID,
      title: 'Evening jog',
      implementedAt: undefined,
      ...overrides,
    });

    beforeEach(() => {
      mockGoalRepository.findById.mockImplementation((id: string) =>
        Promise.resolve(id === NEW_GOAL_ID ? makeGoal({ id: NEW_GOAL_ID, title: 'Run 20km' }) : makeGoal()),
      );
      mockHabitRepository.findByUserId.mockResolvedValue([]);
    });

    it('relinks active goal-linked habits to the new goal when the AI reports related', async () => {
      mockAiService.checkGoalRelevance.mockResolvedValue({ succeeded: true, isRelated: true, reason: 'Same pursuit.' });
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);

      const result = await service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID });

      expect(mockHabitRepository.updateHabit).toHaveBeenCalledWith('habit-1', { goalId: NEW_GOAL_ID });
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
      expect(result).toEqual({ outcome: 'resolved', relinkedHabitIds: ['habit-1'], archivedHabitIds: [] });
    });

    it('archives active goal-linked habits instead when the AI reports unrelated', async () => {
      mockAiService.checkGoalRelevance.mockResolvedValue({ succeeded: true, isRelated: false, reason: 'Different pursuit.' });
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);

      const result = await service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID });

      expect(mockHabitRepository.deleteHabit).toHaveBeenCalledWith('habit-1');
      expect(mockHabitRepository.updateHabit).not.toHaveBeenCalled();
      expect(result).toEqual({ outcome: 'resolved', relinkedHabitIds: [], archivedHabitIds: ['habit-1'] });
    });

    it('leaves already-achieved habits under the old goal untouched either way', async () => {
      mockAiService.checkGoalRelevance.mockResolvedValue({ succeeded: true, isRelated: true, reason: '' });
      mockHabitRepository.findByUserId.mockResolvedValue([
        makeHabit({ implementedAt: new Date().toISOString() }),
      ]);

      const result = await service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID });

      expect(mockHabitRepository.updateHabit).not.toHaveBeenCalled();
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
      expect(result).toEqual({ outcome: 'resolved', relinkedHabitIds: [], archivedHabitIds: [] });
    });

    it('returns needs_decision and touches no habits when the AI call fails', async () => {
      mockAiService.checkGoalRelevance.mockResolvedValue({ succeeded: false, isRelated: true, reason: '' });
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);

      const result = await service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID });

      expect(result).toEqual({ outcome: 'needs_decision', pendingHabitIds: ['habit-1'] });
      expect(mockHabitRepository.updateHabit).not.toHaveBeenCalled();
      expect(mockHabitRepository.deleteHabit).not.toHaveBeenCalled();
    });

    it('applies an explicit link decision directly, without calling the AI', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);

      const result = await service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID, decision: 'link' });

      expect(mockAiService.checkGoalRelevance).not.toHaveBeenCalled();
      expect(mockHabitRepository.updateHabit).toHaveBeenCalledWith('habit-1', { goalId: NEW_GOAL_ID });
      expect(result).toEqual({ outcome: 'resolved', relinkedHabitIds: ['habit-1'], archivedHabitIds: [] });
    });

    it('applies an explicit archive decision directly, without calling the AI', async () => {
      mockHabitRepository.findByUserId.mockResolvedValue([makeHabit()]);

      const result = await service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID, decision: 'archive' });

      expect(mockAiService.checkGoalRelevance).not.toHaveBeenCalled();
      expect(mockHabitRepository.deleteHabit).toHaveBeenCalledWith('habit-1');
      expect(result).toEqual({ outcome: 'resolved', relinkedHabitIds: [], archivedHabitIds: ['habit-1'] });
    });

    it('throws ForbiddenException when the caller does not own the old goal', async () => {
      mockGoalRepository.findById.mockImplementation((id: string) =>
        Promise.resolve(id === NEW_GOAL_ID ? makeGoal({ id: NEW_GOAL_ID }) : makeGoal({ userId: OTHER_USER_ID })),
      );

      await expect(service.resolveHabits(USER_ID, GOAL_ID, { newGoalId: NEW_GOAL_ID })).rejects.toThrow(ForbiddenException);
      expect(mockAiService.checkGoalRelevance).not.toHaveBeenCalled();
    });
  });
});
