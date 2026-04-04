import { Request, Response } from 'express';
import { PersonaRequestDto, PersonaResponseDto } from '../dto/persona.dto';
import { classifyUserPersona } from '../services/aiService';

export async function classifyPersona(req: Request, res: Response) {
  try {
    const { goal, quizAnswers }: PersonaRequestDto = req.body;

    if (!goal || !quizAnswers) {
      return res.status(400).json({ message: 'Missing goal or quizAnswers in request body', success: false });
    }

    const { personaType, motivationalMessage } = await classifyUserPersona(goal, quizAnswers);

    const response: Omit<PersonaResponseDto, 'id'> = {
      personaType,
      motivationalMessage,
      success: true,
    };

    res.status(200).json(response);
  } catch (error) {
    console.error('Error in classifyPersona:', error);
    res.status(500).json({ message: 'Internal Server Error', success: false });
  }
}
