import { Injectable, NotFoundException } from '@nestjs/common';
import { UserRepository } from '../users/user.repository';
import { LeaderboardService } from '../leaderboard/leaderboard.service';
import { HabitRepository } from '../habits/habit.repository';
import { GoalRepository } from '../goals/goal.repository';

@Injectable()
export class TasksService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
    private readonly goalRepository: GoalRepository,
    private readonly leaderboardService: LeaderboardService,
  ) {}

  async completeTask(userId: string, taskId: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const task = [...user.coreGoals, ...user.dailyVariations].find(t => t.id === taskId);
    if (!task) throw new NotFoundException('Task not found');

    const wasAlreadyCompleted = task.completed;

    const found = await this.userRepository.completeTask(userId, taskId);
    if (!found) throw new NotFoundException('Task not found');

    if (!wasAlreadyCompleted) {
      await this.leaderboardService.recordCompletion(userId);

      if (task.genre === 'goal') {
        const activeGoal = await this.goalRepository.findActiveByUserId(userId);
        if (activeGoal) {
          const habits = await this.habitRepository.findByUserId(userId);
          const goalHabit = habits.find((h) => h.goalId === activeGoal.id);
          if (goalHabit) {
            await this.habitRepository.completeHabit(goalHabit.id);
          }
        }
      }
    }

    return { message: 'Task marked as complete', success: true };
  }
}
