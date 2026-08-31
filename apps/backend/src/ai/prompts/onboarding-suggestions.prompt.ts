import { ONBOARDING_QUESTIONS } from '../pillars';
import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface OnboardingSuggestionsPromptInput {
  goal: string;
  // Index i = ONBOARDING_QUESTIONS[i]'s answer so far, "" or missing = not yet answered.
  // Only questions without an answer here get suggestions generated.
  answeredSoFar?: string[];
}

// Only question 1 ("describe a recent goal you set") is actually about the stated goal.
// The rest are deliberately goal-agnostic persona probes (habits stuck with, involving
// others in change, reacting to boredom, deeper purpose, daily structure) meant to sample
// the user's patterns across their whole life - forcing every suggestion to reference the
// same stated goal produced repetitive, disconnected answers (e.g. a "run a marathon" goal
// bleeding into a question about routines feeling stale, when a real answer there is just
// as likely to be about a diet change or a school routine as it is about running).
const GOAL_GROUNDED_QUESTION_ID = 1;

export const buildOnboardingSuggestionsPrompt = ({
  goal,
  answeredSoFar = [],
}: OnboardingSuggestionsPromptInput): string => {
  const answered = ONBOARDING_QUESTIONS
    .map((q, i) => ({ question: q, answer: answeredSoFar[i]?.trim() }))
    .filter((entry): entry is { question: (typeof ONBOARDING_QUESTIONS)[number]; answer: string } =>
      Boolean(entry.answer),
    );
  const remaining = ONBOARDING_QUESTIONS.filter((_, i) => !answeredSoFar[i]?.trim());
  const questionsBlock = remaining.map((q) => `Q${q.id}: ${q.text}`).join('\n');

  const priorAnswersSection = answered.length
    ? `
The user has already answered these earlier questions in this same quiz - treat them as real
signal about who they are. If they mentioned specific pursuits, habits, or interests (e.g.
"learning Italian", "cooking every day"), let suggestions for the remaining questions below
reflect those same specific things where it naturally fits, instead of generic ideas. If they
mentioned more than one distinct pursuit, different suggestion options are free to reference
different ones rather than picking just one.

${answered.map((e) => `Q${e.question.id} (${e.question.text}) — answered: "${e.answer}"`).join('\n')}
`
    : '';

  return `
You are helping a user speed through an onboarding quiz by suggesting quick-pick answers.

${PROMPT_SAFETY_GUARDRAIL}

The user just stated this goal: "${goal}"
${priorAnswersSection}
For EACH of the following background questions, write exactly 3 short, plausible, distinct
sample answers (5-12 words each) that a real person might give.

QUESTIONS
${questionsBlock}

GROUNDING RULES
- Question ${GOAL_GROUNDED_QUESTION_ID} asks directly about a goal the user set, so ground
  its 3 options in the stated goal ("${goal}") — only relevant if it's still listed above.
- Every OTHER question is intentionally about the user's general life patterns, not this
  specific goal — it could just as easily be about school, work, a relationship, diet, or
  any other area of life. Do NOT tie those answers back to "${goal}". Spread the 3 options
  for each of those questions across genuinely different, unrelated life domains so the
  user sees real variety, not the same goal rephrased six times. (The one exception: if the
  user's own prior answers above already connect a question to something specific, reflect
  that instead of inventing something unrelated.)

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
