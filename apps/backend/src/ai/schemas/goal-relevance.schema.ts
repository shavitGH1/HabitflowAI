import { z } from 'zod';

export const goalRelevanceOutputSchema = z.object({
  isRelated: z.boolean(),
  reason: z.string().max(200),
});

export type GoalRelevanceOutput = z.infer<typeof goalRelevanceOutputSchema>;
