import { ONBOARDING_QUESTIONS, PERSONA_TYPES, PILLAR_DEFINITIONS, PILLARS } from '../pillars';

export interface PersonaClassifierPromptInput {
  goal: string;
  openAnswers: string[];
}

export const buildPersonaClassifierPrompt = ({
  goal,
  openAnswers,
}: PersonaClassifierPromptInput): string => {
  const pillarsBlock = PILLAR_DEFINITIONS
    .map((p) => `- ${p.key} (persona: ${p.persona}): ${p.description}`)
    .join('\n');

  const questionsBlock = ONBOARDING_QUESTIONS
    .map((q, i) => `Q${q.id} (primary pillar: ${q.primaryPillar}): ${q.text}\nAnswer: "${openAnswers[i] ?? ''}"`)
    .join('\n\n');

  const personaList = PERSONA_TYPES.map((p) => `"${p}"`).join(', ');
  const pillarKeys = PILLARS.map((p) => `"${p}": <0-100>`).join(', ');

  return `
You are an expert motivational psychologist and habit-formation coach.

STEP 1 — VALIDATE INPUT
The user submitted a long-term goal and free-text answers to 6 open-ended onboarding questions.
If the goal or the answers are gibberish, empty, abusive, or clearly not a genuine attempt
(e.g. "asdf", "test test", random characters), reject the submission.

STEP 2 — SCORE THE 6 MOTIVATIONAL PILLARS
For each of the 6 pillars below, assign an integer score 0-100 representing how strongly
this user is motivated by that pillar, based on the goal and all 6 answers combined.
The 6 scores do NOT need to sum to 100 — score each pillar independently.

Pillars:
${pillarsBlock}

STEP 3 — DETERMINE PERSONA
The user's personaType is the persona corresponding to the highest-scoring pillar.
Allowed values: ${personaList}.

STEP 4 — CONFIDENCE
Provide a confidenceScore between 0 and 1 reflecting how clearly one pillar dominates.
- High confidence (0.8+): one pillar clearly leads.
- Low confidence (<0.5): two or more pillars are tied or close.

INPUT
User's long-term goal: "${goal}"

Open-ended answers (Q1-Q6 in order):
${questionsBlock}

OUTPUT FORMAT
Return STRICT JSON only — no prose, no markdown fences.

If INVALID:
{ "isValid": false, "errorReason": "<short reason in plain English>" }

If VALID:
{
  "isValid": true,
  "personaType": <one of ${personaList}>,
  "weightedBreakdown": { ${pillarKeys} },
  "confidenceScore": <0-1>
}
`.trim();
};
