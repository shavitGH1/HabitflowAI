import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { classifyPersona, generateInitialGoals } from '../services/aiService';
import { findUserById, updateUserPersona } from '../repository/userRepository';
import { ReclassifyDto } from './dto/reclassify.dto';

@Injectable()
export class PersonasService {
  async reclassify(userId: string, { goal, quizAnswers }: ReclassifyDto) {
    const user = findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const classification = await classifyPersona(goal, quizAnswers);
    if (!classification.isValid || !classification.personaType) {
      throw new BadRequestException(`Invalid input: ${classification.errorReason}`);
    }

    const today = new Date();
    const goals = await generateInitialGoals(
      { goal, personaType: classification.personaType, email: user.email },
      today.getDay(),
    );

    const updatedUser = updateUserPersona(userId, {
      goal,
      personaType: classification.personaType,
      motivationalMessage: goals.motivationalMessage ?? '',
      coreGoals: goals.coreGoals?.map(g => ({ ...g, id: uuidv4(), completed: false })) ?? [],
      dailyVariations: goals.dailyVariations?.map(g => ({ ...g, id: uuidv4(), completed: false })) ?? [],
      tasksLastGeneratedDate: today.toISOString().split('T')[0],
    });

    return { user: updatedUser, success: true };
  }
}
