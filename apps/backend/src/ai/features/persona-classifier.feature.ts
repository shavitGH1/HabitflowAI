import { BadRequestException, Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { ONBOARDING_QUESTIONS } from '../pillars';
import { buildPersonaClassifierPrompt } from '../prompts/persona-classifier.prompt';
import {
  PersonaClassifierOutput,
  personaClassifierOutputSchema,
} from '../schemas/persona-classifier.schema';

export interface PersonaClassifierInput {
  goal: string;
  quizAnswers: string[];
}

@Injectable()
export class PersonaClassifierFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async classify({ goal, quizAnswers }: PersonaClassifierInput): Promise<PersonaClassifierOutput> {
    const expected = ONBOARDING_QUESTIONS.length;
    if (quizAnswers.length !== expected) {
      throw new BadRequestException(
        `Expected exactly ${expected} open-ended answers, received ${quizAnswers.length}.`,
      );
    }

    const prompt = buildPersonaClassifierPrompt({ goal, quizAnswers });
    return this.gemini.generateJson(prompt, personaClassifierOutputSchema);
  }
}
