import { NotFoundException } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { UserRepository } from '../users/user.repository';
import { LeaderboardService } from '../leaderboard/leaderboard.service';
import { TasksService } from './tasks.service';

const mockUserRepository = {
  findUserById: jest.fn(),
  completeTask: jest.fn(),
};

const mockLeaderboardService = {
  recordCompletion: jest.fn(),
};

const USER_ID = 'user-123';
const TASK_ID = 'task-abc';

const makeUser = (overrides: Record<string, unknown> = {}) => ({
  id: USER_ID,
  coreGoals: [{ id: TASK_ID, description: 'Morning run', points: 20, completed: false }],
  dailyVariations: [],
  ...overrides,
});

describe('TasksService', () => {
  let service: TasksService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TasksService,
        { provide: UserRepository, useValue: mockUserRepository },
        { provide: LeaderboardService, useValue: mockLeaderboardService },
      ],
    }).compile();

    service = module.get<TasksService>(TasksService);
    jest.clearAllMocks();
  });

  describe('completeTask()', () => {
    it('throws NotFoundException when the user does not exist', async () => {
      mockUserRepository.findUserById.mockResolvedValue(null);

      await expect(service.completeTask(USER_ID, TASK_ID)).rejects.toThrow(NotFoundException);
      expect(mockLeaderboardService.recordCompletion).not.toHaveBeenCalled();
    });

    it('throws NotFoundException when the task id does not match any task', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockUserRepository.completeTask.mockResolvedValue(false);

      await expect(service.completeTask(USER_ID, TASK_ID)).rejects.toThrow(NotFoundException);
      expect(mockLeaderboardService.recordCompletion).not.toHaveBeenCalled();
    });

    it('records a leaderboard completion the first time a task is completed', async () => {
      mockUserRepository.findUserById.mockResolvedValue(makeUser());
      mockUserRepository.completeTask.mockResolvedValue(true);

      const result = await service.completeTask(USER_ID, TASK_ID);

      expect(mockLeaderboardService.recordCompletion).toHaveBeenCalledWith(USER_ID);
      expect(result).toEqual({ message: 'Task marked as complete', success: true });
    });

    it('does not double-record leaderboard points when the task was already completed', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({ coreGoals: [{ id: TASK_ID, description: 'Morning run', points: 20, completed: true }] }),
      );
      mockUserRepository.completeTask.mockResolvedValue(true);

      await service.completeTask(USER_ID, TASK_ID);

      expect(mockLeaderboardService.recordCompletion).not.toHaveBeenCalled();
    });

    it('checks dailyVariations too, not just coreGoals', async () => {
      mockUserRepository.findUserById.mockResolvedValue(
        makeUser({
          coreGoals: [],
          dailyVariations: [{ id: TASK_ID, description: 'Stretch', points: 10, completed: true }],
        }),
      );
      mockUserRepository.completeTask.mockResolvedValue(true);

      await service.completeTask(USER_ID, TASK_ID);

      expect(mockLeaderboardService.recordCompletion).not.toHaveBeenCalled();
    });
  });
});
