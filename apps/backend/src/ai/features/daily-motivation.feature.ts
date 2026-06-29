import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { PersonaType } from '../pillars';
import { buildDailyMotivationPrompt } from '../prompts/daily-motivation-message.prompt';
import {
  DailyMotivationOutput,
  dailyMotivationOutputSchema,
} from '../schemas/daily-motivation.schema';

export interface DailyMotivationInput {
  userId: string;
  personaType: PersonaType;
  currentStreak: number;
  todayCompletionRate: number;
  nearMissDays?: number;
}

@Injectable()
export class DailyMotivationFeature {
  private readonly cache = new Map<string, DailyMotivationOutput>();

  constructor(private readonly gemini: GeminiClient) {}

  async generate(input: DailyMotivationInput): Promise<DailyMotivationOutput> {
    const key = this.cacheKey(input);
    const cached = this.cache.get(key);
    if (cached) return cached;

    const prompt = buildDailyMotivationPrompt(input);
    const output = await this.gemini.generateJson(prompt, dailyMotivationOutputSchema);

    this.cache.set(key, output);
    return output;
  }

  private cacheKey({ userId, personaType }: DailyMotivationInput): string {
    const date = new Date().toISOString().split('T')[0];
    return `${userId}|${date}|${personaType}`;
  }
}
