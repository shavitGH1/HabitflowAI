import { z } from 'zod';

export const dailyMotivationOutputSchema = z.object({
  shortMessage: z.string().min(1).max(80),
  coachingNote: z.string().min(1),
});

export type DailyMotivationOutput = z.infer<typeof dailyMotivationOutputSchema>;
