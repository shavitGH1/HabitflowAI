import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { buildOnboardingSuggestionsPrompt } from '../prompts/onboarding-suggestions.prompt';
import {
  OnboardingSuggestionsOutput,
  onboardingSuggestionsOutputSchema,
} from '../schemas/onboarding-suggestions.schema';

export interface OnboardingSuggestionsInput {
  goal: string;
}

@Injectable()
export class OnboardingSuggestionsFeature {
  constructor(private readonly gemini: GeminiClient) {}

  generate(input: OnboardingSuggestionsInput): Promise<OnboardingSuggestionsOutput> {
    const prompt = buildOnboardingSuggestionsPrompt(input);
    return this.gemini.generateJson(prompt, onboardingSuggestionsOutputSchema);
  }
}
