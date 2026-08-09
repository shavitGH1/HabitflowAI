import { z } from 'zod';

export const taskVerificationOutputSchema = z.object({
  isPlausible: z.boolean(),
  reason: z.string().max(200),
});

export type TaskVerificationOutput = z.infer<typeof taskVerificationOutputSchema>;
