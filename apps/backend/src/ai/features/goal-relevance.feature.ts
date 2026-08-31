import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { logger } from '../../logger';
import { buildGoalRelevancePrompt } from '../prompts/goal-relevance.prompt';
import { goalRelevanceOutputSchema } from '../schemas/goal-relevance.schema';

export interface GoalRelevanceInput {
  oldGoalTitle: string;
  newGoalTitle: string;
}

export interface GoalRelevanceCheckResult {
  succeeded: boolean;
  isRelated: boolean;
  reason: string;
}

const FAILURE_RESULT: GoalRelevanceCheckResult = { succeeded: false, isRelated: true, reason: '' };

@Injectable()
export class GoalRelevanceFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async check(input: GoalRelevanceInput): Promise<GoalRelevanceCheckResult> {
    try {
      const prompt = buildGoalRelevancePrompt(input);
      const result = await this.gemini.generateJson(prompt, goalRelevanceOutputSchema);
      return { succeeded: true, ...result };
    } catch (error) {
      logger.warn({ err: error }, 'goal relevance check failed');
      return FAILURE_RESULT;
    }
  }
}
