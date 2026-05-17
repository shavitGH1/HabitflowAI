import { z } from 'zod';

const goalTaskSchema = z.object({
  description: z.string().min(1),
  points: z.number().int().min(0),
});

export const portfolioGeneratorOutputSchema = z.object({
  summary: z.string().min(1),
  tips: z.array(z.string().min(1)).min(3).max(5),
  failurePatterns: z.array(z.string().min(1)).min(1).max(5),
  coreGoals: z.array(goalTaskSchema).min(3).max(5),
  dailyVariations: z.array(goalTaskSchema).min(1).max(7),
});

export type PortfolioGeneratorOutput = z.infer<typeof portfolioGeneratorOutputSchema>;
export type PortfolioGoalTask = z.infer<typeof goalTaskSchema>;
