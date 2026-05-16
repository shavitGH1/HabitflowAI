# AI Module — HabitFlow AI Backend

Owner: Member 3 (AI Integration / Persona Engine).
Status: Sprint 1 scaffold (2026-05-10 → 2026-05-23). Not yet wired into AuthModule / PersonasModule.

## Layout

```
src/ai/
├── pillars.ts                                 # 6 pillars, persona names, 6 onboarding questions, weight matrix
├── gemini.client.ts                           # @Injectable wrapper around @google/genai, model fallback chain, Zod validation
├── ai.module.ts                               # NestJS module — exports features
├── prompts/
│   ├── persona-classifier.prompt.ts           # buildPrompt({ goal, openAnswers })
│   └── portfolio-generator.prompt.ts          # buildPrompt({ goal, openAnswers, personaType, weightedBreakdown })
├── schemas/
│   ├── persona-classifier.schema.ts           # Zod schema + inferred type
│   └── portfolio-generator.schema.ts
└── features/
    ├── persona-classifier.feature.ts          # @Injectable, calls Gemini + validates
    └── portfolio-generator.feature.ts
```

## Adding a new AI feature

1. Add `src/ai/schemas/<name>.schema.ts` — Zod schema for the Gemini output.
2. Add `src/ai/prompts/<name>.prompt.ts` — a `build<Name>Prompt(input)` function returning a string.
3. Add `src/ai/features/<name>.feature.ts` — `@Injectable` class that calls `gemini.generateJson(prompt, schema)`.
4. Register the feature in `ai.module.ts` (`providers`, `exports`).

No edits to `gemini.client.ts` should be needed for a new feature.

## The 6 motivational pillars

| Pillar | Persona | Onboarding Q (primary) |
|---|---|---|
| Achievement | Achiever | Q1 — recent goal & how progress was measured |
| Growth | Grower | Q2 — habit/skill stuck with for a month |
| Connection | Socializer | Q3 — involving others in change |
| Exploration | Explorer | Q4 — handling stale routines |
| Purpose | Altruist | Q5 — deeper reason for change |
| Structure | Architect | Q6 — ideal daily routine |

Full pillar definitions, question text, and weight matrix live in `pillars.ts`.

## Status against Sprint 1 — Member 3 checklist

- [x] 6 motivational pillars + question→pillar weight mapping (`pillars.ts`)
- [x] `persona-classifier.prompt.ts` (open-ended, 6-pillar 0-100 scoring)
- [x] `portfolio-generator.prompt.ts` (summary, tips, failurePatterns, coreGoals, dailyVariations)
- [x] `PersonaClassifierFeature` module
- [x] `PortfolioGeneratorFeature` module
- [x] Gemini JSON output validation via Zod

Not done in this sprint (deferred per work plan):
- Wiring into AuthService / PersonasService — Member 1 owns this in Sprint 2.
- 20+ fixture sets for the classifier — Sprint 2 task (Member 3).
- DailyMotivationFeature with caching — Sprint 2.
- PersonaDriftDetector + cron — Sprint 3.
