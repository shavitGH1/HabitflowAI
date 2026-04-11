import { Request, Response } from 'express';
import { GenerateGoalsRequest } from '../dto/goal.dto';
import { generateGoals } from '../services/aiService';
import { findUserById } from '../repository/userRepository';

export async function generateGoalsController(req: Request, res: Response) {
  try {
    const { userId, dayOfWeek }: GenerateGoalsRequest = req.body;

    if (!userId || dayOfWeek === undefined) {
      return res.status(400).json({ message: 'Request body must contain userId and dayOfWeek', success: false });
    }

    const user = findUserById(userId);

    if (!user) {
      return res.status(404).json({ message: 'User not found', success: false });
    }

    const aiResponse = await generateGoals(user, dayOfWeek);

    res.status(200).json({ ...aiResponse, success: true });
  } catch (error: any) {
    console.error('Error in generateGoalsController:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}
