import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { PersonaType } from '../pillars';
import { buildHabitInsightsPrompt } from '../prompts/habit-insights.prompt';
import {
  HabitInsightsOutput,
  habitInsightsOutputSchema,
} from '../schemas/habit-insights.schema';

export interface HabitInsightsInput {
  userId: string;
  personaType: PersonaType;
  weekCompletionRate: number;
  currentStreak: number;
  completedHabits: string[];
  missedHabits: string[];
}

@Injectable()
export class HabitInsightsFeature {
  private readonly cache = new Map<string, HabitInsightsOutput>();

  constructor(private readonly gemini: GeminiClient) {}

  async generate(input: HabitInsightsInput): Promise<HabitInsightsOutput> {
    const key = this.cacheKey(input.userId);
    const cached = this.cache.get(key);
    if (cached) return cached;

    const prompt = buildHabitInsightsPrompt(input);
    const output = await this.gemini.generateJson(prompt, habitInsightsOutputSchema);

    this.cache.set(key, output);
    return output;
  }

  private cacheKey(userId: string): string {
    return `${userId}|${isoWeek(new Date())}`;
  }
}

const isoWeek = (date: Date): string => {
  const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = target.getUTCDay() || 7;
  target.setUTCDate(target.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((target.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
  return `${target.getUTCFullYear()}-W${week}`;
};
