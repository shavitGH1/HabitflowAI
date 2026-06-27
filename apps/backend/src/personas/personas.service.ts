import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { AiService } from '../ai/ai.service';
import { UserRepository } from '../users/user.repository';
import { ReclassifyDto } from './dto/reclassify.dto';

@Injectable()
export class PersonasService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly ai: AiService,
  ) {}

  async reclassify(userId: string, { openAnswers }: ReclassifyDto) {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const goal = openAnswers[0];
    const classification = await this.ai.classifyPersonaWeighted({ goal, openAnswers });
    if (!classification.isValid) {
      throw new BadRequestException(`Invalid input: ${classification.errorReason}`);
    }

    const { personaType, weightedBreakdown } = classification;

    await this.userRepository.updateUserPersona(userId, {
      goal,
      personaType,
      personaBreakdown: weightedBreakdown,
      weightedScores: weightedBreakdown,
    });

    return { personaType, weightedBreakdown, success: true };
  }
}
