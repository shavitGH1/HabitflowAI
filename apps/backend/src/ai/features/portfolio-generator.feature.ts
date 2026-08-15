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

    let output: PortfolioGeneratorOutput | undefined;
    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      output = await this.gemini.generateJson(prompt, portfolioGeneratorOutputSchema);
      const offTopic = await this.findOffTopicGoalTasks(input.goal, output);
      if (offTopic.length === 0) return output;

      logger.warn(
        {
          goal: input.goal,
          offTopic: offTopic.map((t) => t.description),
          attempt,
        },
        'portfolio generator produced goal-tagged tasks unrelated to the stated goal, regenerating',
      );
    }

    // Never block onboarding on this — same soft-warning philosophy as habit-goal relevance
    // elsewhere in the app. Return the last attempt even if it still has an off-topic task.
    return output as PortfolioGeneratorOutput;
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
