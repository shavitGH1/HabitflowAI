import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { logger } from '../../logger';
import { buildGoalRelevancePrompt } from '../prompts/goal-relevance.prompt';
import { GoalRelevanceOutput, goalRelevanceOutputSchema } from '../schemas/goal-relevance.schema';

export interface GoalRelevanceInput {
  oldGoalTitle: string;
  newGoalTitle: string;
}

const FALLBACK_OUTPUT: GoalRelevanceOutput = { isRelated: true, reason: '' };

@Injectable()
export class GoalRelevanceFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async check(input: GoalRelevanceInput): Promise<GoalRelevanceOutput> {
    try {
      const prompt = buildGoalRelevancePrompt(input);
      return await this.gemini.generateJson(prompt, goalRelevanceOutputSchema);
    } catch (error) {
      logger.warn({ err: error }, 'goal relevance check failed, defaulting to related');
      return FALLBACK_OUTPUT;
    }
  }
}
