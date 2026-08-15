import { CoachStats } from './coach.rules';
import { ProposalContext, rejectionReason } from './coach.policy';

const makeStats = (overrides: Partial<CoachStats> = {}): CoachStats => ({
  habitCount: 3,
  completionRate7d: 0.6,
  streak: 2,
  oldestHabitAgeDays: 40,
  completedToday: [],
  notesToday: [],
  ...overrides,
});

const makeContext = (overrides: Partial<ProposalContext> = {}): ProposalContext => ({
  stats: makeStats(),
  drift: { checked: false, detected: false, suggestedPersona: null },
  activeGoalId: null,
  goalIsFailing: false,
  alreadyStaged: false,
  ...overrides,
});

describe('coach proposal policy', () => {
  it('allows only one staged change per conversation', () => {
    const reason = rejectionReason(
      { type: 'adjustDifficulty', rationale: 'r', direction: 'decrease' },
      makeContext({ stats: makeStats({ completionRate7d: 0.1 }), alreadyStaged: true }),
    );

    expect(reason).toMatch(/already staged/);
  });

  describe('personaSwitch', () => {
    const change = { type: 'personaSwitch' as const, rationale: 'r', suggestedPersona: 'Grower' as const };

    it('requires the drift check to have run first', () => {
      expect(rejectionReason(change, makeContext())).toMatch(/check_persona_drift/);
    });

    it('rejects when the drift check found nothing', () => {
      const context = makeContext({
        drift: { checked: true, detected: false, suggestedPersona: null },
      });

      expect(rejectionReason(change, context)).toMatch(/no meaningful change/);
    });

    it('rejects a persona the drift check did not suggest', () => {
      const context = makeContext({
        drift: { checked: true, detected: true, suggestedPersona: 'Architect' },
      });

      expect(rejectionReason(change, context)).toMatch(/"Architect", not "Grower"/);
    });

    it('allows the exact persona the drift check suggested', () => {
      const context = makeContext({
        drift: { checked: true, detected: true, suggestedPersona: 'Grower' },
      });

      expect(rejectionReason(change, context)).toBeNull();
    });
  });

  describe('adjustDifficulty', () => {
    const harder = { type: 'adjustDifficulty' as const, rationale: 'r', direction: 'increase' as const };
    const easier = { type: 'adjustDifficulty' as const, rationale: 'r', direction: 'decrease' as const };

    it('rejects any adjustment when the user has no habits', () => {
      const context = makeContext({ stats: makeStats({ habitCount: 0 }) });

      expect(rejectionReason(easier, context)).toMatch(/no habits yet/);
    });

    it('rejects a harder plan below the 80% threshold and names the real rate', () => {
      const context = makeContext({ stats: makeStats({ completionRate7d: 0.62 }) });

      expect(rejectionReason(harder, context)).toMatch(/at least 80%.*at 62%/);
    });

    it('allows a harder plan at exactly 80%', () => {
      const context = makeContext({ stats: makeStats({ completionRate7d: 0.8 }) });

      expect(rejectionReason(harder, context)).toBeNull();
    });

    it('rejects an easier plan at or above the 50% threshold', () => {
      const context = makeContext({ stats: makeStats({ completionRate7d: 0.5 }) });

      expect(rejectionReason(easier, context)).toMatch(/below a 50%/);
    });

    it('allows an easier plan below 50%', () => {
      const context = makeContext({ stats: makeStats({ completionRate7d: 0.3 }) });

      expect(rejectionReason(easier, context)).toBeNull();
    });
  });

  describe('forfeitGoal', () => {
    const change = { type: 'forfeitGoal' as const, rationale: 'r', goalId: 'goal-1' };

    it('rejects when there is no active goal', () => {
      expect(rejectionReason(change, makeContext())).toMatch(/no active goal/);
    });

    it('rejects a goal id that is not the active one', () => {
      const context = makeContext({ activeGoalId: 'goal-2', goalIsFailing: true });

      expect(rejectionReason(change, context)).toMatch(/Only the active goal/);
    });

    it('rejects a goal whose habits are not in a failure pattern', () => {
      const context = makeContext({ activeGoalId: 'goal-1', goalIsFailing: false });

      expect(rejectionReason(change, context)).toMatch(/not in a sustained failure pattern/);
    });

    it('allows forfeiting the active goal once its habits are failing', () => {
      const context = makeContext({ activeGoalId: 'goal-1', goalIsFailing: true });

      expect(rejectionReason(change, context)).toBeNull();
    });
  });
});
