import { z } from 'zod';

export const goalTaskSchema = z.object({
  description: z.string().min(1),
  points: z.number().int().min(0),
  genre: z.enum(['goal', 'persona', 'habit']),
  habitId: z.string().optional(),
});

export const dailyVariationsSchema = z
  .array(goalTaskSchema)
  .min(4)
  .refine(
    (tasks) => {
      const goalCount = tasks.filter((t) => t.genre === 'goal').length;
      const personaCount = tasks.filter((t) => t.genre === 'persona').length;
      return goalCount === 2 && personaCount === 2;
    },
    { message: 'dailyVariations must contain exactly 2 "goal" tasks and 2 "persona" tasks, plus habit tasks' },
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
