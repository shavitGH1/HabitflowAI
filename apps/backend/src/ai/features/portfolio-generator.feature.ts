import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { Pillar, PersonaType } from '../pillars';
import { buildPortfolioGeneratorPrompt } from '../prompts/portfolio-generator.prompt';
import { PortfolioGeneratorOutput, portfolioGeneratorOutputSchema } from '../schemas/portfolio-generator.schema';

export interface PortfolioGeneratorInput {
  goal: string;
  openAnswers: string[];
  personaType: PersonaType;
  weightedBreakdown: Record<Pillar, number>;
}

@Injectable()
export class PortfolioGeneratorFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async generate(input: PortfolioGeneratorInput): Promise<PortfolioGeneratorOutput> {
    const prompt = buildPortfolioGeneratorPrompt(input);

    // Trust the primary prompt's strict relevance rules to save quota, rather than
    // regenerating on a separate per-task relevance check.
    return await this.gemini.generateJson(prompt, portfolioGeneratorOutputSchema);
  }
}
