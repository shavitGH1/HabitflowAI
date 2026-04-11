import { Request, Response } from 'express';
import { ClassifyPersonaRequest, ClassifyPersonaResponse } from '../dto/persona.dto';
import { classifyPersona } from '../services/aiService';
import { saveUser } from '../repository/userRepository';

export async function classifyPersonaController(req: Request, res: Response) {
  try {
    const { goal, quizAnswers }: ClassifyPersonaRequest = req.body;

    if (!goal || !quizAnswers) {
      return res.status(400).json({ message: 'Request body must contain goal and quizAnswers', success: false });
    }

    const classificationResult = await classifyPersona(goal, quizAnswers);

    if (!classificationResult.isValid || !classificationResult.personaType) {
      return res.status(400).json({ message: `Invalid input: ${classificationResult.errorReason}`, success: false });
    }

    const user = saveUser(goal, classificationResult.personaType);

    const response: ClassifyPersonaResponse = {
      id: user.id,
      personaType: user.personaType,
      success: true,
    };

    res.status(200).json(response);
  } catch (error: any) {
    console.error('Error in classifyPersonaController:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}
