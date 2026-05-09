import { Injectable, NotFoundException } from '@nestjs/common';
import { findUserById } from '../repository/userRepository';

@Injectable()
export class TasksService {
  completeTask(userId: string, taskId: string) {
    const user = findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const task =
      user.coreGoals.find(t => t.id === taskId) ??
      user.dailyVariations.find(t => t.id === taskId);
    if (!task) throw new NotFoundException('Task not found');

    task.completed = true;
    return { message: 'Task marked as complete', success: true };
  }
}
