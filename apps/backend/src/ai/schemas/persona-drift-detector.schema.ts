import { z } from 'zod';
import { PERSONA_TYPES, PILLARS } from '../pillars';

const pillarBreakdownSchema = z.object(
  Object.fromEntries(
    PILLARS.map((p) => [p, z.number().int().min(0).max(100)]),
  ) as Record<(typeof PILLARS)[number], z.ZodNumber>,
);

export const driftDetectorOutputSchema = z.object({
  currentBreakdown: pillarBreakdownSchema,
  suggestedPersona: z.enum(PERSONA_TYPES),
  rationale: z.string().min(1),
});

export type DriftDetectorOutput = z.infer<typeof driftDetectorOutputSchema>;
