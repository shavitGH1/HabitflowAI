import { PILLAR_DEFINITIONS, PersonaType } from '../pillars';

export interface HabitInsightsPromptInput {
  personaType: PersonaType;
  weekCompletionRate: number;
  currentStreak: number;
  completedHabits: string[];
  missedHabits: string[];
}

export const buildHabitInsightsPrompt = ({
  personaType,
  weekCompletionRate,
  currentStreak,
  completedHabits,
  missedHabits,
}: HabitInsightsPromptInput): string => {
  const personaInfo = PILLAR_DEFINITIONS.find((p) => p.persona === personaType);
  const personaDescription = personaInfo?.description ?? '';

  const completed = completedHabits.length
    ? completedHabits.map((h) => `"${h}"`).join(', ')
    : 'none';
  const missed = missedHabits.length ? missedHabits.map((h) => `"${h}"`).join(', ') : 'none';

  return `
You are a habit coach writing a user's weekly progress review.

USER CONTEXT
Persona: ${personaType} — ${personaDescription}
This week's completion rate: ${Math.round(weekCompletionRate * 100)}%
Current streak: ${currentStreak} day(s)
Habits completed this week: ${completed}
Habits missed this week: ${missed}

TASK
Write a short, encouraging weekly review tailored to the ${personaType} persona.
Cover what went well and what to improve, grounded in the actual habits above.

OUTPUT
Return STRICT JSON only — no prose, no markdown fences. Schema:

{
  "summary": "<2-3 sentence weekly summary in second person>",
  "wins": ["<concrete win 1>", "..."],            // 1 to 3 entries
  "improvements": ["<actionable improvement 1>", "..."]  // 1 to 3 entries
}

RULES
- Reference the actual habits where possible — not generic advice.
- Encouraging tone, no guilt-tripping. Plain English, no emojis, no markdown.
`.trim();
};
