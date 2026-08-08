import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { logger } from '../../logger';
import { buildHabitGoalRelevancePrompt } from '../prompts/habit-goal-relevance.prompt';
import {
  HabitGoalRelevanceOutput,
  habitGoalRelevanceOutputSchema,
} from '../schemas/habit-goal-relevance.schema';

export interface HabitGoalRelevanceInput {
  goalTitle: string;
  habitTitle: string;
  habitDescription?: string;
}

const FALLBACK_OUTPUT: HabitGoalRelevanceOutput = { isRelated: true, reason: '' };

@Injectable()
export class HabitGoalRelevanceFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async check(input: HabitGoalRelevanceInput): Promise<HabitGoalRelevanceOutput> {
    try {
      const prompt = buildHabitGoalRelevancePrompt(input);
      return await this.gemini.generateJson(prompt, habitGoalRelevanceOutputSchema);
    } catch (error) {
      logger.warn({ err: error }, 'habit-goal relevance check failed, defaulting to related');
      return FALLBACK_OUTPUT;
    }
  }
}
