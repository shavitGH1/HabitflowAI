import { z } from 'zod';

export const coachPhrasingOutputSchema = z.object({
  message: z.string().min(1).max(300),
});

export type CoachPhrasingOutput = z.infer<typeof coachPhrasingOutputSchema>;
