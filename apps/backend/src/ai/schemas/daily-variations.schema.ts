import { z } from 'zod';
import { dailyVariationsSchema } from './portfolio-generator.schema';

export const dailyVariationsOutputSchema = z.object({
  dailyVariations: dailyVariationsSchema,
});

export type DailyVariationsOutput = z.infer<typeof dailyVariationsOutputSchema>;
