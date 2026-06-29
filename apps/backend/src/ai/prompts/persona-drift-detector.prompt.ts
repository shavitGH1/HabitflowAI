import { PERSONA_TYPES, PILLARS, PILLAR_DEFINITIONS, Pillar, PersonaType } from '../pillars';

export interface BehaviorSnapshot {
  observationWindowDays: number;
  recentCompletionRate: number;
  activeStreak: number;
  completedHabits: string[];
  skippedHabits: string[];
  positiveFeedbackCount?: number;
  negativeFeedbackCount?: number;
}

export interface DriftDetectorPromptInput {
  currentPersona: PersonaType;
  baselineBreakdown: Record<Pillar, number>;
  behaviorSnapshot: BehaviorSnapshot;
}

export const buildDriftDetectorPrompt = ({
  currentPersona,
  baselineBreakdown,
  behaviorSnapshot,
}: DriftDetectorPromptInput): string => {
  const pillarsBlock = PILLAR_DEFINITIONS
    .map((p) => `- ${p.key} (persona: ${p.persona}): ${p.description}`)
    .join('\n');

  const baselineBlock = Object.entries(baselineBreakdown)
    .map(([pillar, score]) => `- ${pillar}: ${score}/100`)
    .join('\n');

  const completed = behaviorSnapshot.completedHabits.length
    ? behaviorSnapshot.completedHabits.map((h) => `"${h}"`).join(', ')
    : 'none';
  const skipped = behaviorSnapshot.skippedHabits.length
    ? behaviorSnapshot.skippedHabits.map((h) => `"${h}"`).join(', ')
    : 'none';

  const personaList = PERSONA_TYPES.map((p) => `"${p}"`).join(', ');
  const pillarKeys = PILLARS.map((p) => `"${p}": <0-100>`).join(', ');

  return `
You are a behavioral analyst detecting whether a user's motivational profile is shifting.

THE 6 MOTIVATIONAL PILLARS
${pillarsBlock}

BASELINE PROFILE (from onboarding)
Current persona: ${currentPersona}
Baseline pillar breakdown (0-100):
${baselineBlock}

OBSERVED BEHAVIOR (last ${behaviorSnapshot.observationWindowDays} days)
Completion rate: ${Math.round(behaviorSnapshot.recentCompletionRate * 100)}%
Active streak: ${behaviorSnapshot.activeStreak} day(s)
Habits completed: ${completed}
Habits skipped/abandoned: ${skipped}
Positive motivation feedback: ${behaviorSnapshot.positiveFeedbackCount ?? 0}
Negative motivation feedback: ${behaviorSnapshot.negativeFeedbackCount ?? 0}

TASK
Infer the user's CURRENT motivational pillar breakdown based ONLY on the observed behavior
above (not the baseline). Score each of the 6 pillars 0-100 independently. Then pick the
persona that best matches the highest-scoring current pillar.

OUTPUT
Return STRICT JSON only — no prose, no markdown fences. Schema:

{
  "currentBreakdown": { ${pillarKeys} },
  "suggestedPersona": <one of ${personaList}>,
  "rationale": "<one short sentence explaining the inferred shift, or lack of it>"
}
`.trim();
};
