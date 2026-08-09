import { Injectable, NotFoundException } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { AiService } from '../ai/ai.service';
import { UserRepository } from './user.repository';

@Injectable()
export class UsersService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly ai: AiService,
  ) {}

  async getAllUsers(): Promise<{ id: string; email: string }[]> {
    const users = await this.userRepository.findAllUsers();
    return users.map(u => ({ id: u.id, email: u.email }));
  }

  async getHomePageData(userId: string) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const today = new Date().toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== today) {
      const newDailyTasks = await this.ai.generateDailyVariations(user, new Date().getDay());
      const updatedTasks = newDailyTasks.map(t => ({ ...t, id: uuidv4(), completed: false }));
      const updated = await this.userRepository.updateUserDailyTasks(userId, updatedTasks);
      user.dailyVariations = updated?.dailyVariations ?? updatedTasks;
    }

    return {
      goal: user.goal,
      personaType: user.personaType,
      motivationalMessage: user.motivationalMessage,
      coreGoals: user.coreGoals,
      dailyVariations: user.dailyVariations,
      portfolioSummary: user.portfolioSummary,
      tips: user.tips,
      failurePatterns: user.failurePatterns,
      confidenceScore: user.confidenceScore,
      success: true,
    };
  }
}
