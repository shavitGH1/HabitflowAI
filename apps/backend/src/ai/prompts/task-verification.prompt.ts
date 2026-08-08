import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface TaskVerificationPromptInput {
  habitTitle: string;
  note: string;
}

export const buildTaskVerificationPrompt = ({ habitTitle, note }: TaskVerificationPromptInput): string => {
  return `
You are checking whether a user's note plausibly describes completing a specific habit.

${PROMPT_SAFETY_GUARDRAIL}

HABIT
"${habitTitle}"

USER'S NOTE (what they say they did)
"${note}"

TASK
Decide if the note plausibly describes completing this habit today. Be generous - people
describe the same action in many different ways. Only flag it as implausible if the note
clearly describes something unrelated to the habit.

OUTPUT
Return STRICT JSON only - no prose, no markdown fences. Schema:

{
  "isPlausible": <true or false>,
  "reason": "<one short sentence, max 200 characters, explaining the verdict>"
}
`.trim();
};
