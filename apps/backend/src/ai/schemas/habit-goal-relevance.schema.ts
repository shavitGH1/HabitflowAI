import { z } from 'zod';

export const habitGoalRelevanceOutputSchema = z.object({
  isRelated: z.boolean(),
  reason: z.string().max(200),
});

export type HabitGoalRelevanceOutput = z.infer<typeof habitGoalRelevanceOutputSchema>;
