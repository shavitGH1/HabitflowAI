import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { GoalData, GoalRepository } from './goal.repository';
import { UserRepository } from '../users/user.repository';
import { GoalsService, GOAL_ACHIEVEMENT_MEDAL } from './goals.service';

const mockGoalRepository = {
  createGoal: jest.fn(),
  findActiveByUserId: jest.fn(),
  findById: jest.fn(),
  updateStatus: jest.fn(),
};

const mockUserRepository = {
  addAchievement: jest.fn(),
  findUserById: jest.fn(),
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
      mockGoalRepository.createGoal.mockResolvedValue(created);

      const result = await service.getActiveGoal(USER_ID);

      expect(mockGoalRepository.createGoal).toHaveBeenCalledWith({
        userId: USER_ID,
        title: 'Run a marathon',
        targetDate: expect.any(Date),
      });
      expect(result).toEqual(created);
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
});
