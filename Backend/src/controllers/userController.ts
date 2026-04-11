import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/authMiddleware';
import { findUserById, updateUserDailyTasks } from '../repository/userRepository';
import { generateDailyVariations } from '../services/aiService';
import { v4 as uuidv4 } from 'uuid';

export async function getHomePageData(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized', success: false });
    }

    const user = findUserById(userId);
    if (!user) {
      return res.status(404).json({ message: 'User not found', success: false });
    }

    const today = new Date().toISOString().split('T')[0];
    if (user.tasksLastGeneratedDate !== today) {
      console.log('Generating new daily tasks for user:', userId);
      const dayOfWeek = new Date().getDay();
      const newDailyTasks = await generateDailyVariations(user, dayOfWeek);
      const updatedTasks = newDailyTasks.map(task => ({ ...task, id: uuidv4(), completed: false }));
      updateUserDailyTasks(userId, updatedTasks);
      user.dailyVariations = updatedTasks; // Update user object for the response
    }

    res.status(200).json({
      goal: user.goal,
      personaType: user.personaType,
      motivationalMessage: user.motivationalMessage,
      coreGoals: user.coreGoals,
      dailyVariations: user.dailyVariations,
      success: true,
    });
  } catch (error: any) {
    console.error('Error in getHomePageData:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}
