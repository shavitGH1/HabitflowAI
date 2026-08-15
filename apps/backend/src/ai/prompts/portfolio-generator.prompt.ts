import { ONBOARDING_QUESTIONS, PILLAR_DEFINITIONS, Pillar, PersonaType } from '../pillars';
import { PROMPT_SAFETY_GUARDRAIL } from './safety';

export interface PortfolioGeneratorPromptInput {
  goal: string;
  openAnswers: string[];
  personaType: PersonaType;
  weightedBreakdown: Record<Pillar, number>;
}

export const buildPortfolioGeneratorPrompt = ({
  goal,
  openAnswers,
  personaType,
  weightedBreakdown,
}: PortfolioGeneratorPromptInput): string => {
  const breakdownBlock = Object.entries(weightedBreakdown)
    .map(([pillar, score]) => `- ${pillar}: ${score}/100`)
    .join('\n');

  const personaInfo = PILLAR_DEFINITIONS.find((p) => p.persona === personaType);
  const personaDescription = personaInfo?.description ?? '';

  const answersBlock = ONBOARDING_QUESTIONS
    .map((q, i) => `Q${q.id}: ${q.text}\nA: "${openAnswers[i] ?? ''}"`)
    .join('\n\n');

  return `
You are an expert habit coach producing a personalized motivational portfolio.

${PROMPT_SAFETY_GUARDRAIL}

CONTEXT
The user has just completed onboarding. Their persona has been classified from a background
quiz, and you have their pillar breakdown. Generate a portfolio that speaks directly to this
person — tone matched to their dominant pillar — while staying focused on the goal they
actually asked for.

PRIMARY GOAL (authoritative — this alone defines what "goal"-tagged content is about)
"${goal}"

Persona: ${personaType} — ${personaDescription}

Pillar breakdown (0-100):
${breakdownBlock}

BACKGROUND CONTEXT (tone/persona signal only — NEVER task subject matter)
These are free-text answers to generic background questions, asked before the user stated
their goal above. They describe past habits, motivations, and tendencies unrelated to this
goal. Use them only to flavor tone, phrasing, and the 2 persona-tagged dailyVariations below.
Do NOT lift any activity, habit, or subject named in these answers into a "goal"-tagged task —
a "goal" task's subject must come exclusively from the PRIMARY GOAL above, even if an answer
mentions a specific, concrete, well-known activity.
${answersBlock}

OUTPUT
Return STRICT JSON only — no prose, no markdown fences. Schema:

{
  "summary": "<2-3 sentence motivational profile summary in second person ('You...')>",
  "tips": ["<tip 1>", "<tip 2>", "<tip 3>"],            // 3 to 5 entries, specific to their answers
  "failurePatterns": ["<pattern 1>", "..."],            // 1 to 5 likely failure modes for this persona
  "coreGoals": [                                         // 3 to 5 long-term habits supporting the goal
    { "description": "<habit description>", "points": <integer 10-50>, "genre": "goal" }
  ],
  "dailyVariations": [                                   // exactly 4 day-one daily tasks
    { "description": "<task>", "points": <integer 5-30>, "genre": "goal" | "persona" }
  ]
}

RULES
- Tips must reference details from the user's background answers — not generic advice.
- Failure patterns must reflect typical weaknesses of the ${personaType} persona.
- coreGoals are long-term habits (multi-week) that directly support the PRIMARY GOAL above,
  in subject matter and wording — tag every coreGoals entry "genre": "goal". If a background
  answer's topic has no connection to the primary goal, it must not appear in any coreGoals entry.
- dailyVariations must contain exactly 4 entries: 2 tagged "genre": "goal" (concrete day-one
  actions that directly advance the PRIMARY GOAL above — never the subject of a background
  answer) and 2 tagged "genre": "persona" (general habits that build the ${personaType}
  persona's strengths — these may draw on the background answers, independent of the specific goal).
- Points reflect difficulty/importance — keep them roughly proportional.
- Write in plain English, second person, no emojis, no markdown formatting inside strings.
`.trim();
};
