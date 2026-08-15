# Coach Bot — Feature Specification

Author: Member 3 (AI / Persona Engine)
Status: Implemented
Scope: `apps/backend/src/coach/`, the `coach-phrasing` and `coaching-agent` features under `src/ai/`

## 1. Concept

A coach bot is a seeded user that participates in a normal 1:1 chat with every user.
It works in both directions:

| Direction | Trigger | Engine |
|---|---|---|
| Proactive | daily and weekly cron | deterministic rules, Gemini rewrites the wording |
| Reactive | the user sends a message | tool-calling agent loop, may propose a change |

Proactively it posts a short daily summary of what the user completed, a weekly grade with one
tip, and — once the user has 30 days of tracked history — an optional persona-switch suggestion.
Reactively it answers questions and can propose a concrete change that the user must confirm.

Everything lands in one chat thread, so the transcript is the single record of the coaching
relationship.

The proactive path separates **what to say** from **how to say it**. A deterministic rule engine
picks the message using fixed numeric thresholds, and a Gemini phrasing layer rewrites that
decision in the user's persona tone. Every AI call has a static template fallback, so the bot
still works — and still says the same thing — when the model is unavailable.

The existing Gemini-based `GET /ai/weekly-insights` endpoint stays untouched.

## 2. Why it lives inside the chat module

The bot is registered as an ordinary participant in the existing `Chat` document.
This removes almost all client work:

| Concern | How it is solved |
|---|---|
| Chat list | Coach chat is returned by the existing `GET /chats` because the bot is a participant |
| Real-time delivery | Existing `ChatGateway.emitToRoom` |
| Unread badge | Existing `unreadCount` map on `Chat` |
| Message storage | Existing `Message` schema |
| Android UI | No new screen — the existing chat screen renders it |

`ChatService.createChat` validates that every participant is a real user, so the bot must
exist as a real `User` document with a fixed `_id`.

## 3. The rule engine

This section covers the proactive path only. The conversational path is section 3.6.

Two inputs, both already present on `HabitData`:

| Input | Source |
|---|---|
| `completionRate7d` | `completionHistory` entries in the last 7 days / (habit count × 7) |
| `streak` | max `streak` across the user's active habits |

### 3.1 Bands

| Band | Condition |
|---|---|
| `EXCELLENT` | `rate >= 0.8` |
| `GOOD` | `0.5 <= rate < 0.8` |
| `SLIPPING` | `0.2 <= rate < 0.5` |
| `AT_RISK` | `rate < 0.2` |

### 3.2 Message composition

A message is a band sentence plus a persona flavour line — 4 + 6 = 10 strings total,
not a 24-cell matrix.

```
<band sentence>  +  <persona line for user.personaType>  [+ <tip> on weekly runs]
```

Persona lines cover the six personas defined in `src/ai/pillars.ts`:
`Achiever`, `Grower`, `Socializer`, `Explorer`, `Altruist`, `Architect`.

### 3.3 Tip selection

Ordered rules, first match wins. Weekly messages only.

| # | Condition | Tip theme |
|---|---|---|
| 1 | `streak === 0` | Restart small — commit to one habit tomorrow |
| 2 | `streak > 0 && rate < 0.5` | You held one habit but dropped others — reduce the list |
| 3 | `rate >= 0.8` | You are stable — consider adding one habit |

### 3.4 Persona-switch suggestion

Runs inside the weekly job. Gate: the user's oldest habit was created at least 30 days ago.
When the gate passes, the bot calls the existing `PersonasService.driftCheck`. If
`driftDetected` is true, one extra line naming `newSuggestedPersona` is appended.

The 30-day age is derived from `HabitData.createdAt` rather than `User.createdAt`, so the
shared `UserData` interface does not change.

### 3.5 Phrasing layer (LLM)

The rule engine produces a `CoachDecision`:

```
{ band, tipId?, personaType, stats, suggestedPersona? }
```

That decision — never the raw habit data — is passed to a Gemini feature that returns a single
rewritten sentence. The model may change wording and tone; it may not change the verdict.

| Layer | Decides | On failure |
|---|---|---|
| `coach.rules.ts` | band, tip, persona-switch | n/a — pure functions |
| `coach-phrasing.feature.ts` | wording and tone | falls back to the static template |

Rules for the AI call, following the existing `src/ai/` conventions:

- Output validated with a Zod schema — one `message` field, 300 characters maximum.
- `PROMPT_SAFETY_GUARDRAIL` from `src/ai/prompts/safety.ts` is included in the prompt.
- Cached per `userId|date|band`, matching `DailyMotivationFeature`.
- Any API error, malformed JSON, schema violation or empty response results in the static
  template string from `coach.templates.ts` being used verbatim.

Because the fallback is the template, the 10 static strings remain the ground truth of the
feature and are what the unit tests assert against.

### 3.6 Conversational path

When the user sends a message, `CoachService.converse` writes it into the coach chat, runs the
coach agent, and writes the reply back into the same chat.

The agent starts with no user context in its prompt. It gathers what it needs by calling tools,
one turn at a time, until it stops asking for tools or hits the step limit. Each tool has one
responsibility and its description names the sibling tool to use instead, so two tools never
answer the same question:

| Tool | Answers | Explicitly not for |
|---|---|---|
| `get_progress_summary` | overall standing: rate, streak, band, verdict sentence | naming individual habits |
| `get_habit_list` | per-habit streak, consistency, linked goal | judging overall progress |
| `get_persona_profile` | the stored persona and its pillar scores | whether that persona still fits |
| `get_active_goal` | the current goal and its exact id | anything about habits |
| `check_persona_drift` | whether the persona still fits (expensive, cached per conversation) | routine questions |
| `propose_change` | stages ONE change for the user to confirm | applying anything |

Each turn the model may emit function calls; the loop executes them, feeds the results back as
`functionResponse` parts, and asks again. The last ten messages of the coach chat are replayed
as history, so the thread itself is the agent's memory. After `maxSteps` (default 5) the loop
makes one final call with no tools attached, which guarantees a text answer.

### 3.7 Deterministic boundary

The same split as the proactive path applies here: **code decides, the model phrases.**

`get_progress_summary` does not hand the model raw numbers to interpret. It returns the band and
the verdict sentence already chosen by `coach.rules.ts`, and the prompt tells the model to
rephrase them rather than re-score the user.

`propose_change` is a proposal, not a decision. Every call is re-checked by `coach.policy.ts`, a
pure function over fixed thresholds:

| Proposal | Allowed only when |
|---|---|
| `personaSwitch` | `check_persona_drift` ran in this conversation, detected drift, and named exactly that persona |
| `adjustDifficulty` `increase` | 7-day completion rate ≥ 80% |
| `adjustDifficulty` `decrease` | 7-day completion rate < 50% |
| `forfeitGoal` | the id matches the active goal **and** its linked habits are all at streak 0 with average consistency < 30% |
| any | no change staged yet in this conversation |

A rejected proposal throws `ToolRejectedError`, and the loop returns the reason to the model as
`{ rejected: ... }` — distinct from `{ error: ... }`, which means the tool actually broke. The
model is told why the data does not support it and answers honestly instead of the proposal
being dropped behind its back. Note what this ordering enforces: the model cannot propose a
persona switch without first paying for the drift analysis.

### 3.8 Fallback

The model is never the only thing standing between the user and an answer:

| Failure | What the user gets |
|---|---|
| One tool throws | `{ error }` back to the model, which reports what it could not check |
| A proposal violates policy | `{ rejected }` back to the model, with the threshold it missed |
| Gemini fails on one model | the next model in `GeminiClient`'s chain |
| Every model fails, or the reply is empty | the deterministic weekly summary from `coach.rules.ts` |
| Even the database is unreachable | the static `COACH_OFFLINE_REPLY` line |

The main fallback is real coaching, not an apology: `CoachToolSession.fallbackReply` recomputes
the user's stats and returns the same band sentence, persona line and tip the weekly cron would
have produced. A staged proposal is dropped on that path, because the reply explaining it never
reached the user.

## 4. Module layout

```
apps/backend/src/coach/
  coach.templates.ts    bot id, 4 band sentences, 6 persona lines, 3 tips, fallback and confirmation lines
  coach.rules.ts        computeStats / pickBand / pickTip / daily+weekly summaries  (pure)
  coach.policy.ts       rejectionReason — the fixed thresholds every proposal is re-checked against (pure)
  coach.toolset.ts      per-user tool session: the 6 coach tools, staged proposal, fallback reply
  coach.agent.ts        runs the agent loop with the coach system instruction
  coach.service.ts      ensureCoachChat, postDaily, postWeekly, converse, confirmChange, @Cron
  coach.controller.ts   check-in, weekly-review, chat, chat/confirm
  coach.module.ts
  coach.rules.spec.ts
  coach.policy.spec.ts
  coach.toolset.spec.ts

apps/backend/src/ai/
  agent/agent-tool.ts       AgentTool contract, defineTool (Zod-validated args), ToolRejectedError
  agent/agent-loop.ts       generic tool-calling loop, no domain knowledge
  agent/agent-loop.spec.ts
  prompts/coach-agent.prompt.ts
  prompts/coach-phrasing.prompt.ts
  schemas/coach-phrasing.schema.ts
  schemas/coaching-agent.schema.ts
  features/coach-phrasing.feature.ts
  features/coach-phrasing.feature.spec.ts
```

`coach.rules.ts` and `coach.policy.ts` have no framework, database or AI imports, so every
threshold in the feature is unit-tested directly against pure functions.
`agent-loop.ts` knows nothing about habits or personas — it only knows tools, turns and errors,
and `GeminiClient.generateWithTools` is the single place the SDK's function-calling API is used.
The coach tools are the only place that maps repositories onto that contract.

`CoachService` is the only writer to the coach chat. `PersonasService` owns `driftCheck` and
`confirmCoachChange` and knows nothing about chat.

## 5. API

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/coach/chat` | Send a message; both sides are stored in the coach chat. Returns the reply, an optional `proposedChange` and `toolsUsed` |
| `POST` | `/api/v1/coach/chat/confirm` | Apply a change the coach proposed |
| `POST` | `/api/v1/coach/check-in` | Run the daily job for the caller immediately |
| `POST` | `/api/v1/coach/weekly-review` | Run the weekly job for the caller immediately |

All guarded by `JwtAuthGuard`; the two conversational routes also use `ThrottlerGuard`.
The last two exist so the feature can be demonstrated without waiting for a cron window.

Reading coach messages uses the existing chat endpoints. No new read API.

## 6. Scheduling

| Job | Cron | Notes |
|---|---|---|
| Daily summary | `0 20 * * *` | 20:00 local |
| Weekly review | `0 18 * * 0` | Sunday 18:00 |

Monday 09:00 is deliberately avoided — `PersonaDriftScheduler` already owns that slot.

Idempotency: a run is skipped when the most recent coach message in the chat was already
created today. No new collection or flag is introduced.

## 7. Reused without modification

`ChatService.postMessage`, `ChatService.getMessages`, `ChatService.assertParticipant`,
`ChatGateway.emitToRoom`, `HabitRepository.findByUserId`, `GoalRepository.findActiveByUserId`,
`UserRepository.findAllUsers`, `PersonasService.driftCheck`, `PersonasService.confirmCoachChange`,
`GeminiClient`, `PROMPT_SAFETY_GUARDRAIL`.

## 8. Task assignment

### Member 1 — Backend / Infra

1. In `apps/backend/src/chat/chat.module.ts`, add `exports: [ChatService, ChatGateway]`.
   Without this the coach module cannot post messages.
2. In `mongo-init.js`, seed the coach user with a fixed 24-character hex `_id`, a
   non-login password hash, and a display name. The id must match `BOT_USER_ID` in
   `coach.templates.ts`.

### Member 2 — Android UI

The coach chat renders through the existing chat screen with no changes. To use the
conversational path, send the user's text to `POST /coach/chat`; when the response contains a
`proposedChange`, show a confirm action that calls `POST /coach/chat/confirm`. Never apply a
proposed change without an explicit user tap.

### Member 3 — AI / Persona Engine

Owns the whole `src/coach/` module — the rule engine, the templates, the cron jobs, the
controller and the tests — plus the `coach-phrasing` and `coaching-agent` prompts, schemas and
features under `src/ai/`.

### Member 4 — Android Data Layer

No required work for this version. Existing chat sync covers the coach chat, including messages
the user sends to the coach.

## 9. Out of scope

Quick-reply buttons, push notifications, group coach chats, new drift mathematics, and any
change to the `Chat`, `Message` or `User` schemas.

The phrasing model rewrites messages and never decides the verdict. The conversational agent may
propose a change but can never apply one without explicit user confirmation.

## 10. Verification

1. `coach.rules.spec.ts` — table-driven cases for the four bands plus edge cases:
   no habits, empty completion history, zero streak.
2. `coach-phrasing.feature.spec.ts` — mocked `GeminiClient`, asserting that a valid response is
   returned as-is and that an API error, malformed JSON and an over-length message each fall back
   to the static template.
3. `POST /coach/check-in` returns a created message, and `unreadCount` increments for the user.
4. `POST /coach/chat` stores the user's message and the reply in the same chat.
5. `pnpm test` and `pnpm build` pass from the repo root.
