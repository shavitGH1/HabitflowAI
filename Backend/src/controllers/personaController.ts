import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/authMiddleware';
import { classifyPersona, generateInitialGoals } from '../services/aiService';
import { findUserById, updateUserPersona } from '../repository/userRepository';
import { v4 as uuidv4 } from 'uuid';

export async function reclassifyPersona(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const { goal, quizAnswers } = req.body;

    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized', success: false });
    }

    if (!goal || !quizAnswers) {
      return res.status(400).json({ message: 'Request body must contain goal and quizAnswers', success: false });
    }

    const user = findUserById(userId);
    if (!user) {
      return res.status(404).json({ message: 'User not found', success: false });
    }

    const classificationResult = await classifyPersona(goal, quizAnswers);
    if (!classificationResult.isValid || !classificationResult.personaType) {
      return res.status(400).json({ message: `Invalid input: ${classificationResult.errorReason}`, success: false });
    }

    const today = new Date();
    const goals = await generateInitialGoals({ goal, personaType: classificationResult.personaType, email: user.email }, today.getDay());

    const updatedUser = updateUserPersona(userId, {
      goal,
      personaType: classificationResult.personaType,
      motivationalMessage: goals.motivationalMessage || '',
      coreGoals: goals.coreGoals?.map(g => ({ ...g, id: uuidv4(), completed: false })) || [],
      dailyVariations: goals.dailyVariations?.map(g => ({ ...g, id: uuidv4(), completed: false })) || [],
      tasksLastGeneratedDate: today.toISOString().split('T')[0],
    });

    res.status(200).json({ user: updatedUser, success: true });
  } catch (error: any) {
    console.error('Error in reclassifyPersona:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}
