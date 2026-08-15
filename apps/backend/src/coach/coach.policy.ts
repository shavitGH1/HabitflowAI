import { PersonaType } from '../ai/pillars';
import { ProposedChange } from '../ai/schemas/coaching-agent.schema';
import { CoachStats } from './coach.rules';

export const EASIER_MAX_RATE = 0.5;
export const HARDER_MIN_RATE = 0.8;

export interface DriftFinding {
  checked: boolean;
  detected: boolean;
  suggestedPersona: PersonaType | null;
}

export interface ProposalContext {
  stats: CoachStats;
  drift: DriftFinding;
  activeGoalId: string | null;
  goalIsFailing: boolean;
  alreadyStaged: boolean;
}

export const rejectionReason = (change: ProposedChange, context: ProposalContext): string | null => {
  if (context.alreadyStaged) {
    return 'A change is already staged in this conversation. Only one proposal is allowed per reply.';
  }

  switch (change.type) {
    case 'personaSwitch':
      return personaSwitchRejection(change.suggestedPersona, context.drift);
    case 'adjustDifficulty':
      return difficultyRejection(change.direction, context.stats);
    case 'forfeitGoal':
      return forfeitRejection(change.goalId, context);
  }
};

const personaSwitchRejection = (proposed: PersonaType, drift: DriftFinding): string | null => {
  if (!drift.checked) {
    return 'Call check_persona_drift before proposing a persona switch.';
  }
  if (!drift.detected || !drift.suggestedPersona) {
    return 'The drift analysis found no meaningful change, so a persona switch is not supported by the data.';
  }
  if (drift.suggestedPersona !== proposed) {
    return `The drift analysis pointed to "${drift.suggestedPersona}", not "${proposed}".`;
  }
  return null;
};

const difficultyRejection = (
  direction: 'increase' | 'decrease',
  { habitCount, completionRate7d }: CoachStats,
): string | null => {
  if (!habitCount) return 'The user has no habits yet, so there is no plan to adjust.';

  const percent = Math.round(completionRate7d * 100);
  if (direction === 'increase' && completionRate7d < HARDER_MIN_RATE) {
    return `A harder plan needs a 7-day completion rate of at least ${HARDER_MIN_RATE * 100}%. The user is at ${percent}%.`;
  }
  if (direction === 'decrease' && completionRate7d >= EASIER_MAX_RATE) {
    return `An easier plan is only offered below a ${EASIER_MAX_RATE * 100}% completion rate. The user is at ${percent}%.`;
  }
  return null;
};

const forfeitRejection = (
  goalId: string,
  { activeGoalId, goalIsFailing }: ProposalContext,
): string | null => {
  if (!activeGoalId) return 'The user has no active goal to forfeit.';
  if (activeGoalId !== goalId) {
    return 'Only the active goal can be forfeited. Call get_active_goal for the correct id.';
  }
  if (!goalIsFailing) {
    return 'The habits linked to this goal are not in a sustained failure pattern, so forfeiting is not supported.';
  }
  return null;
};
