import { PILLAR_DEFINITIONS, PersonaType } from '../pillars';
import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface CoachPhrasingPromptInput {
  personaType: PersonaType;
  baseMessage: string;
  completionRate7d: number;
  streak: number;
}

export const buildCoachPhrasingPrompt = ({
  personaType,
  baseMessage,
  completionRate7d,
  streak,
}: CoachPhrasingPromptInput): string => {
  const personaDescription = PILLAR_DEFINITIONS.find((p) => p.persona === personaType)?.description ?? '';

  return `
You are rewriting a habit coach message so it sounds natural for one specific user.

${PROMPT_SAFETY_GUARDRAIL}

USER CONTEXT
Persona: ${personaType} — ${personaDescription}
Completion rate over the last 7 days: ${Math.round(completionRate7d * 100)}%
Current streak: ${streak} day(s)

MESSAGE TO REWRITE
"${baseMessage}"

TASK
Rewrite the message above in the tone of the ${personaType} persona.

OUTPUT
Return STRICT JSON only — no prose, no markdown fences. Schema:

{
  "message": "<the rewritten message>"
}

RULES
- Keep the exact same verdict and the same advice. Do not soften or upgrade the assessment.
- Do not invent habit names, numbers, dates or achievements that are not in the message above.
- Keep it under 300 characters, second person, plain English, no emojis, no markdown.
- Sound like a real coach talking to this person, not a status report. Use contractions and
  everyday words. The persona shapes what advice resonates with them, not how mechanical the
  sentence sounds — never turn persona traits into literal jargon (e.g. do not say "system
  efficiency" just because the persona values structure; say it the way a supportive person
  who values structure would actually talk).
`.trim();
};
