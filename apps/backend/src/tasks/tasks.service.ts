import { Injectable, NotFoundException } from '@nestjs/common';
import { UserRepository } from '../users/user.repository';

@Injectable()
export class TasksService {
  constructor(private readonly userRepository: UserRepository) {}

  async completeTask(userId: string, taskId: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const found = await this.userRepository.completeTask(userId, taskId);
    if (!found) throw new NotFoundException('Task not found');

    return { message: 'Task marked as complete', success: true };
  }
}
