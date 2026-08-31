import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface GoalRelevancePromptInput {
  oldGoalTitle: string;
  newGoalTitle: string;
}

export const buildGoalRelevancePrompt = ({
  oldGoalTitle,
  newGoalTitle,
}: GoalRelevancePromptInput): string => {
  return `
You are checking whether a user's new goal is a natural continuation of a goal they just
resolved (achieved or abandoned), or a completely different pursuit.

${PROMPT_SAFETY_GUARDRAIL}

RESOLVED GOAL
"${oldGoalTitle}"

NEW GOAL
"${newGoalTitle}"

TASK
Decide if the new goal is a continuation, escalation, or close variant of the resolved one
(e.g. "Run 10km under 40 minutes" -> "Run 20km" or "Run 10km under 35 minutes" are related).
Only mark it as unrelated if it is a genuinely different pursuit.

OUTPUT
Return STRICT JSON only - no prose, no markdown fences. Schema:

{
  "isRelated": <true or false>,
  "reason": "<one short sentence, max 200 characters, explaining the verdict>"
}
`.trim();
};
