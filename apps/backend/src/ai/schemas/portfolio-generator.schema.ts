import { z } from 'zod';

export const goalTaskSchema = z.object({
  description: z.string().min(1),
  points: z.number().int().min(0),
  genre: z.enum(['goal', 'persona', 'habit']),
  habitId: z.string().nullable().optional(),
});

export const dailyVariationsSchema = z
  .array(goalTaskSchema)
  .min(3)
  .refine(
    (tasks) => {
      const goalCount = tasks.filter((t) => t.genre === 'goal').length;
      return goalCount >= 3 && goalCount <= 5;
    },
    { message: 'dailyVariations must contain between 3 and 5 "goal" tasks' },
  );

export const portfolioGeneratorOutputSchema = z.object({
  summary: z.string().min(1),
  tips: z.array(z.string().min(1)).min(3).max(5),
  failurePatterns: z.array(z.string().min(1)).min(1).max(5),
  coreGoals: z.array(goalTaskSchema).min(3).max(5),
  dailyVariations: dailyVariationsSchema,
});

export type PortfolioGeneratorOutput = z.infer<typeof portfolioGeneratorOutputSchema>;
export type PortfolioGoalTask = z.infer<typeof goalTaskSchema>;
