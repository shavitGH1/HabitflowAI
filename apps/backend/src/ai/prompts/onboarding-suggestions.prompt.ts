import { ONBOARDING_QUESTIONS } from '../pillars';
import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface OnboardingSuggestionsPromptInput {
  goal: string;
}

export const buildOnboardingSuggestionsPrompt = ({ goal }: OnboardingSuggestionsPromptInput): string => {
  const questionsBlock = ONBOARDING_QUESTIONS.map((q) => `Q${q.id}: ${q.text}`).join('\n');

  return `
You are helping a user speed through an onboarding quiz by suggesting quick-pick answers.

${PROMPT_SAFETY_GUARDRAIL}

The user just stated this goal: "${goal}"

For EACH of the following background questions, write exactly 3 short, plausible, distinct
sample answers (5-12 words each) that a real person pursuing this specific goal might give.
Ground every suggestion in the stated goal ("${goal}") — do not write generic answers that
could apply to any goal.

QUESTIONS
${questionsBlock}

OUTPUT
Return STRICT JSON only — no prose, no markdown fences. Schema:

{
  "suggestions": [
    { "questionId": <number>, "options": ["<option 1>", "<option 2>", "<option 3>"] }
  ]
}

RULES
- One entry per question above, in the same order, each with exactly 3 options.
- The user will tap an option to prefill a text field they can still edit — write each
  option as a natural first-person sentence fragment, not a keyword or label.
- Options for the same question must be meaningfully different from each other.
`.trim();
};
