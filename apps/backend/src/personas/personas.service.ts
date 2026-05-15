import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { classifyPersona, generateInitialGoals } from '../services/aiService';
import { UserRepository } from '../users/user.repository';
import { ReclassifyDto } from './dto/reclassify.dto';

@Injectable()
export class PersonasService {
  constructor(private readonly userRepository: UserRepository) {}

  async reclassify(userId: string, { goal, quizAnswers }: ReclassifyDto) {
    const user = await this.userRepository.findUserById(userId);
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

    const updatedUser = await this.userRepository.updateUserPersona(userId, {
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
