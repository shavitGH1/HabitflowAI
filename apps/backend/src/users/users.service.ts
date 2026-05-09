import { Injectable, NotFoundException } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { generateDailyVariations } from '../services/aiService';
import { findUserById, updateUserDailyTasks } from '../repository/userRepository';

@Injectable()
export class UsersService {
  async getHomePageData(userId: string) {
    const user = findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const today = new Date().toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== today) {
      const newDailyTasks = await generateDailyVariations(user, new Date().getDay());
      const updatedTasks = newDailyTasks.map(t => ({ ...t, id: uuidv4(), completed: false }));
      updateUserDailyTasks(userId, updatedTasks);
      user.dailyVariations = updatedTasks;
    }

    return {
      goal: user.goal,
      personaType: user.personaType,
      motivationalMessage: user.motivationalMessage,
      coreGoals: user.coreGoals,
      dailyVariations: user.dailyVariations,
      success: true,
    };
  }
}
