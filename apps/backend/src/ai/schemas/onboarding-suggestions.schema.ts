import { z } from 'zod';
import { ONBOARDING_QUESTIONS } from '../pillars';

export const onboardingSuggestionSchema = z.object({
  questionId: z.number().int(),
  options: z.array(z.string().min(1)).length(3),
});

// Parametric so a mid-quiz refresh (only the remaining, not-yet-answered questions) can
// require just that count instead of always all of them.
export const buildOnboardingSuggestionsOutputSchema = (expectedCount: number) =>
  z.object({
    suggestions: z.array(onboardingSuggestionSchema).length(expectedCount),
  });

export const onboardingSuggestionsOutputSchema = buildOnboardingSuggestionsOutputSchema(
  ONBOARDING_QUESTIONS.length,
);

export type OnboardingSuggestion = z.infer<typeof onboardingSuggestionSchema>;
export type OnboardingSuggestionsOutput = z.infer<typeof onboardingSuggestionsOutputSchema>;
