import { z } from 'zod';

export const habitInsightsOutputSchema = z.object({
  summary: z.string().min(1),
  wins: z.array(z.string().min(1)).min(1).max(3),
  improvements: z.array(z.string().min(1)).min(1).max(3),
});

export type HabitInsightsOutput = z.infer<typeof habitInsightsOutputSchema>;
