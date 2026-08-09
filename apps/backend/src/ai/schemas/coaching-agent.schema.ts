// STOPGAP (Nir): The proposedChanges:
// field names (type/suggestedPersona/direction/goalId) are a shared contract with
// PersonasService.sanitizeProposedChange and ConfirmCoachChangeDto — coordinate before renaming.
import { z } from 'zod';
import { PERSONA_TYPES } from '../pillars';

const personaSwitchChangeSchema = z.object({
  type: z.literal('personaSwitch'),
  rationale: z.string().min(1).max(300),
  suggestedPersona: z.enum(PERSONA_TYPES),
});

const adjustDifficultyChangeSchema = z.object({
  type: z.literal('adjustDifficulty'),
  rationale: z.string().min(1).max(300),
  direction: z.enum(['increase', 'decrease']),
});

const forfeitGoalChangeSchema = z.object({
  type: z.literal('forfeitGoal'),
  rationale: z.string().min(1).max(300),
  goalId: z.string().min(1),
});

export const proposedChangeSchema = z.discriminatedUnion('type', [
  personaSwitchChangeSchema,
  adjustDifficultyChangeSchema,
  forfeitGoalChangeSchema,
]);

export type ProposedChange = z.infer<typeof proposedChangeSchema>;

export const coachingAgentOutputSchema = z.object({
  reply: z.string().min(1).max(1000),
  proposedChange: proposedChangeSchema.nullish(),
});

export type CoachingAgentOutput = z.infer<typeof coachingAgentOutputSchema>;

export interface ResolvedCoachingAgentOutput {
  reply: string;
  proposedChange: ProposedChange | null;
}
