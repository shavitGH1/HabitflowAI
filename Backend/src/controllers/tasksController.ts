import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/authMiddleware';
import { findUserById } from '../repository/userRepository';

export async function completeTask(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const { taskId } = req.params;

    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized', success: false });
    }

    const user = findUserById(userId);
    if (!user) {
      return res.status(404).json({ message: 'User not found', success: false });
    }

    const task = user.coreGoals.find(t => t.id === taskId) || user.dailyVariations.find(t => t.id === taskId);
    if (!task) {
      return res.status(404).json({ message: 'Task not found', success: false });
    }

    task.completed = true;

    res.status(200).json({ message: 'Task marked as complete', success: true });
  } catch (error: any) {
    console.error('Error in completeTask:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}
