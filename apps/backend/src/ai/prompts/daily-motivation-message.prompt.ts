import { PILLAR_DEFINITIONS, PersonaType } from '../pillars';

export interface DailyMotivationPromptInput {
  personaType: PersonaType;
  currentStreak: number;
  todayCompletionRate: number;
  nearMissDays?: number;
}

export const buildDailyMotivationPrompt = ({
  personaType,
  currentStreak,
  todayCompletionRate,
  nearMissDays = 0,
}: DailyMotivationPromptInput): string => {
  const personaInfo = PILLAR_DEFINITIONS.find((p) => p.persona === personaType);
  const personaDescription = personaInfo?.description ?? '';
  const completionPct = Math.round(todayCompletionRate * 100);

  return `
You are a personal habit coach writing a daily motivational nudge.

USER CONTEXT
Persona: ${personaType} — ${personaDescription}
Current streak: ${currentStreak} day(s)
Today's task completion: ${completionPct}%
Near-miss days in the last week (almost completed but fell short): ${nearMissDays}

TASK
Write one motivational message tailored to this persona's psychology and today's numbers.
- If the streak is strong, reinforce momentum.
- If completion is low or there were near-miss days, be encouraging without guilt-tripping.
- Match the tone to the ${personaType} persona.

OUTPUT
Return STRICT JSON only — no prose, no markdown fences. Schema:

{
  "shortMessage": "<punchy message, MAX 80 characters, no emojis>",
  "coachingNote": "<2-3 sentence coaching note expanding on the short message>"
}

RULES
- shortMessage MUST be 80 characters or fewer.
- Second person ("You..."). Plain English. No emojis, no markdown.
`.trim();
};
