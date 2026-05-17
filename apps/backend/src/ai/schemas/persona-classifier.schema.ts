import { z } from 'zod';
import { PERSONA_TYPES, PILLARS } from '../pillars';

const pillarBreakdownSchema = z.object(
  Object.fromEntries(
    PILLARS.map((p) => [p, z.number().int().min(0).max(100)]),
  ) as Record<(typeof PILLARS)[number], z.ZodNumber>,
);

export const personaClassifierOutputSchema = z.discriminatedUnion('isValid', [
  z.object({
    isValid: z.literal(false),
    errorReason: z.string().min(1),
  }),
  z.object({
    isValid: z.literal(true),
    personaType: z.enum(PERSONA_TYPES),
    weightedBreakdown: pillarBreakdownSchema,
    confidenceScore: z.number().min(0).max(1),
  }),
]);

export type PersonaClassifierOutput = z.infer<typeof personaClassifierOutputSchema>;
