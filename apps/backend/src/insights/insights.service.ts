import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { AiService } from '../ai/ai.service';
import { PERSONA_TYPES, PersonaType } from '../ai/pillars';
import { HabitInsightsOutput } from '../ai/schemas/habit-insights.schema';
import { FeedbackTally, MotivationVote } from '../ai/feedback/motivation-feedback.store';
import { UserRepository } from '../users/user.repository';

@Injectable()
export class InsightsService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly ai: AiService,
  ) {}

  async getWeeklyInsights(userId: string): Promise<HabitInsightsOutput> {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');

    const personaType = this.toPersonaType(user.personaType);
    if (!personaType) {
      throw new BadRequestException(`User has an unknown persona: ${user.personaType}`);
    }

    const tasks = [...user.coreGoals, ...user.dailyVariations];
    const completed = tasks.filter((t) => t.completed);
    const missed = tasks.filter((t) => !t.completed);

    return this.ai.getWeeklyInsights({
      userId: user.id,
      personaType,
      weekCompletionRate: tasks.length ? completed.length / tasks.length : 0,
      currentStreak: 0,
      completedHabits: completed.map((t) => t.description),
      missedHabits: missed.map((t) => t.description),
    });
  }

  async recordFeedback(userId: string, vote: MotivationVote): Promise<FeedbackTally> {
    const user = await this.userRepository.findUserById(userId);
    if (!user) throw new NotFoundException('User not found');
    return this.ai.recordMotivationFeedback(userId, vote);
  }

  private toPersonaType(value: string): PersonaType | null {
    return (PERSONA_TYPES as readonly string[]).includes(value) ? (value as PersonaType) : null;
  }
}
