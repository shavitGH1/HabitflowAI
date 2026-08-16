import { Injectable, NotFoundException } from '@nestjs/common';
import { UserRepository } from '../users/user.repository';
import { LeaderboardService } from '../leaderboard/leaderboard.service';

@Injectable()
export class TasksService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly leaderboardService: LeaderboardService,
  ) {}

  async completeTask(userId: string, taskId: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    // completeTask() re-sets completed:true unconditionally even if it already
    // was — check the prior state so leaderboard points aren't awarded twice
    // for repeat calls on an already-completed task.
    const wasAlreadyCompleted = [...user.coreGoals, ...user.dailyVariations]
      .find(t => t.id === taskId)?.completed ?? false;

    const found = await this.userRepository.completeTask(userId, taskId);
    if (!found) throw new NotFoundException('Task not found');

    if (!wasAlreadyCompleted) {
      await this.leaderboardService.recordCompletion(userId);
    }

    return { message: 'Task marked as complete', success: true };
  }
}
