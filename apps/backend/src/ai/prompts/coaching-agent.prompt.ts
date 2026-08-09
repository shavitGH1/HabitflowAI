// STOPGAP (Nir): prompt wording owned by Yaron freely
import { PersonaType } from '../pillars';
import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface CoachingAgentHabitContext {
  id: string;
  title: string;
  goalId?: string;
  consistencyScore: number;
  streak: number;
}

export interface CoachingAgentGoalContext {
  id: string;
  title: string;
  targetDate: string;
}

export interface CoachingAgentPromptInput {
  message: string;
  personaType: PersonaType | null;
  activeGoal: CoachingAgentGoalContext | null;
  habits: CoachingAgentHabitContext[];
  driftSuggestedPersona: PersonaType | null;
}

export const buildCoachingAgentPrompt = ({
  message,
  personaType,
  activeGoal,
  habits,
  driftSuggestedPersona,
}: CoachingAgentPromptInput): string => {
  const habitsBlock = habits.length
    ? habits
        .map(
          (h) =>
            `- "${h.title}" (streak: ${h.streak} days, consistency: ${Math.round(h.consistencyScore * 100)}%, ${
              h.goalId ? 'linked to the active goal' : 'standalone'
            })`,
        )
        .join('\n')
    : '(no habits yet)';

  const goalBlock = activeGoal
    ? `Active goal: "${activeGoal.title}" (id: "${activeGoal.id}", target date: ${activeGoal.targetDate})`
    : 'No active goal.';

  const driftBlock = driftSuggestedPersona
    ? `Recent behavior drift suggests the user may now fit the "${driftSuggestedPersona}" persona better than their current one.`
    : 'No persona drift detected recently.';

  return `
You are a supportive habit-coaching assistant embedded in HabitFlow AI.

${PROMPT_SAFETY_GUARDRAIL}

USER CONTEXT
Current persona: "${personaType ?? 'unknown'}"
${goalBlock}
Habits:
${habitsBlock}
${driftBlock}

USER MESSAGE
"${message}"

TASK
Reply conversationally and helpfully to the user's message, using the context above. If — and
only if — the context clearly supports it, you may propose ONE change for the user to explicitly
accept or decline. Never propose a change the user didn't ask about or that isn't clearly
supported by the context above. Do not invent a goal id — reuse the exact active goal id given
above, verbatim.

Allowed proposedChange types (set proposedChange to null if none clearly applies):
- "personaSwitch": suggest switching persona. Only propose this if a drift suggestion is present
  above. suggestedPersona must be exactly one of: Achiever, Grower, Socializer, Explorer,
  Altruist, Architect.
- "adjustDifficulty": suggest making today's tasks easier or harder. direction must be
  "increase" (clear recent consistency/streaks) or "decrease" (repeated recent misses).
- "forfeitGoal": suggest forfeiting the active goal. Only propose this if there is an active goal
  and its linked habits show a consistent pattern of failure. goalId must exactly match the
  active goal id given above.

OUTPUT
Return STRICT JSON only - no prose, no markdown fences. Schema:

{
  "reply": "<conversational reply, max 1000 characters>",
  "proposedChange": null | {
    "type": "personaSwitch" | "adjustDifficulty" | "forfeitGoal",
    "rationale": "<one short sentence, max 300 characters>",
    "suggestedPersona": "<only when type is personaSwitch>",
    "direction": "<only when type is adjustDifficulty>",
    "goalId": "<only when type is forfeitGoal>"
  }
}
`.trim();
};
