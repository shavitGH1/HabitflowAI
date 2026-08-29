import { Injectable } from '@nestjs/common';
import { logger } from '../../logger';
import { GeminiClient } from '../gemini.client';
import { Pillar, PersonaType } from '../pillars';
import { buildPortfolioGeneratorPrompt } from '../prompts/portfolio-generator.prompt';
import {
  PortfolioGeneratorOutput,
  PortfolioGoalTask,
  portfolioGeneratorOutputSchema,
} from '../schemas/portfolio-generator.schema';
import { HabitGoalRelevanceFeature } from './habit-goal-relevance.feature';

export interface PortfolioGeneratorInput {
  goal: string;
  openAnswers: string[];
  personaType: PersonaType;
  weightedBreakdown: Record<Pillar, number>;
}

const MAX_ATTEMPTS = 3;

@Injectable()
export class PortfolioGeneratorFeature {
  constructor(
    private readonly gemini: GeminiClient,
    private readonly habitGoalRelevance: HabitGoalRelevanceFeature,
  ) {}

  async generate(input: PortfolioGeneratorInput): Promise<PortfolioGeneratorOutput> {
    const prompt = buildPortfolioGeneratorPrompt(input);

    // Efficiency Fix: We trust the primary prompt's strict relevance rules to save quota.
    // This reduces AI calls from 10+ per refresh down to just 1.
    return await this.gemini.generateJson(prompt, portfolioGeneratorOutputSchema);
  }

  private async findOffTopicGoalTasks(
    goal: string,
    output: PortfolioGeneratorOutput,
  ): Promise<PortfolioGoalTask[]> {
    const goalTaggedTasks = [
      ...output.coreGoals,
      ...output.dailyVariations.filter((task) => task.genre === 'goal'),
    ];

    const verdicts = await Promise.all(
      goalTaggedTasks.map((task) =>
        this.habitGoalRelevance.check({ goalTitle: goal, habitTitle: task.description }),
      ),
    );

    return goalTaggedTasks.filter((_, index) => !verdicts[index].isRelated);
  }
}
