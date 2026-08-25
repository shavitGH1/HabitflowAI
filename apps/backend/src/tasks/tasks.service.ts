import { Injectable, NotFoundException } from '@nestjs/common';
import { UserRepository } from '../users/user.repository';
import { LeaderboardService } from '../leaderboard/leaderboard.service';
import { HabitRepository } from '../habits/habit.repository';

@Injectable()
export class TasksService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly habitRepository: HabitRepository,
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

      // If it's a goal-genre task, we also mark the primary goal habit as complete for today.
      // This connects the Home dashboard actions to the actual consistency score logic.
      if (task.genre === 'goal') {
        const habits = await this.habitRepository.findByUserId(userId);
        const goalHabit = habits.find((h) => h.title === user.goal);
        if (goalHabit) {
          await this.habitRepository.completeHabit(goalHabit.id);
        }
      }
    }

    return { message: 'Task marked as complete', success: true };
  }
}
