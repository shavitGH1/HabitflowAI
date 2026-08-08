import { Injectable } from '@nestjs/common';
import { logger } from '../../logger';
import { GeminiClient } from '../gemini.client';
import { PersonaType } from '../pillars';
import { buildCoachPhrasingPrompt } from '../prompts/coach-phrasing.prompt';
import { coachPhrasingOutputSchema } from '../schemas/coach-phrasing.schema';

export interface CoachPhrasingInput {
  userId: string;
  personaType: PersonaType;
  baseMessage: string;
  completionRate7d: number;
  streak: number;
  cacheTag: string;
}

@Injectable()
export class CoachPhrasingFeature {
  private readonly cache = new Map<string, string>();

  constructor(private readonly gemini: GeminiClient) {}

  async phrase(input: CoachPhrasingInput): Promise<string> {
    const key = this.cacheKey(input);
    const cached = this.cache.get(key);
    if (cached) return cached;

    try {
      const output = await this.gemini.generateJson(
        buildCoachPhrasingPrompt(input),
        coachPhrasingOutputSchema,
      );
      this.cache.set(key, output.message);
      return output.message;
    } catch (error) {
      logger.warn({ userId: input.userId, err: error }, 'coach phrasing failed, falling back to template');
      return input.baseMessage;
    }
  }

  private cacheKey({ userId, cacheTag }: CoachPhrasingInput): string {
    return `${userId}|${new Date().toISOString().split('T')[0]}|${cacheTag}`;
  }
}
