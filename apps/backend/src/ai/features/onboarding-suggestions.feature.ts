import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { ONBOARDING_QUESTIONS } from '../pillars';
import { buildOnboardingSuggestionsPrompt } from '../prompts/onboarding-suggestions.prompt';
import {
  OnboardingSuggestionsOutput,
  buildOnboardingSuggestionsOutputSchema,
} from '../schemas/onboarding-suggestions.schema';

export interface OnboardingSuggestionsInput {
  goal: string;
  answeredSoFar?: string[];
}

@Injectable()
export class OnboardingSuggestionsFeature {
  constructor(private readonly gemini: GeminiClient) {}

  generate(input: OnboardingSuggestionsInput): Promise<OnboardingSuggestionsOutput> {
    const remainingCount = ONBOARDING_QUESTIONS.filter((_, i) => !input.answeredSoFar?.[i]?.trim()).length;
    if (remainingCount === 0) return Promise.resolve({ suggestions: [] });

    const prompt = buildOnboardingSuggestionsPrompt(input);
    return this.gemini.generateJson(prompt, buildOnboardingSuggestionsOutputSchema(remainingCount));
  }
}
