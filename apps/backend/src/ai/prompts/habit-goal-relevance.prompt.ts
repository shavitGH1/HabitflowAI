import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface HabitGoalRelevancePromptInput {
  goalTitle: string;
  habitTitle: string;
  habitDescription?: string;
}

export const buildHabitGoalRelevancePrompt = ({
  goalTitle,
  habitTitle,
  habitDescription,
}: HabitGoalRelevancePromptInput): string => {
  return `
You are checking whether a habit a user wants to link to their goal actually supports
that goal.

${PROMPT_SAFETY_GUARDRAIL}

GOAL
"${goalTitle}"

HABIT TO LINK
Title: "${habitTitle}"
${habitDescription ? `Description: "${habitDescription}"` : ''}

TASK
Decide if this habit plausibly supports progress toward the goal. Be generous - habits
can support a goal indirectly (e.g. "Stretch 10 minutes" can support "Run a marathon").
Only flag it as unrelated if there is no reasonable connection at all.

OUTPUT
Return STRICT JSON only - no prose, no markdown fences. Schema:

{
  "isRelated": <true or false>,
  "reason": "<one short sentence, max 200 characters, explaining the verdict>"
}
`.trim();
};
