import { z } from 'zod';
import { ONBOARDING_QUESTIONS } from '../pillars';

export const onboardingSuggestionSchema = z.object({
  questionId: z.number().int(),
  options: z.array(z.string().min(1)).length(3),
});

export const onboardingSuggestionsOutputSchema = z.object({
  suggestions: z.array(onboardingSuggestionSchema).length(ONBOARDING_QUESTIONS.length),
});

export type OnboardingSuggestion = z.infer<typeof onboardingSuggestionSchema>;
export type OnboardingSuggestionsOutput = z.infer<typeof onboardingSuggestionsOutputSchema>;
