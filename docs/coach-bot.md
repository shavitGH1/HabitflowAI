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
| Reactive | the user sends a message | conversational agent, may propose a change |

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

When the user sends a message, `CoachService.converse` writes it into the coach chat, calls
`PersonasService.coachChat`, and writes the reply back into the same chat.

The agent sees the user's persona, active goal, habits with their consistency scores and streaks,
and any pending drift suggestion. It returns a reply and optionally a `proposedChange`:
`personaSwitch`, `adjustDifficulty` or `forfeitGoal`.

A proposed change is never applied on its own. The client must call the confirm endpoint, and the
bot then posts a confirmation line so the transcript records what was agreed. A `forfeitGoal`
proposal that does not match the user's active goal is dropped before it reaches the client.

On any model failure the agent returns a fixed apology reply with no proposed change.

## 4. Module layout

```
apps/backend/src/coach/
  coach.templates.ts    bot id, 4 band sentences, 6 persona lines, 3 tips, confirmation lines
  coach.rules.ts        computeStats / pickBand / pickTip  (pure, no Nest, no DB)
  coach.service.ts      ensureCoachChat, postDaily, postWeekly, converse, confirmChange, @Cron
  coach.controller.ts   check-in, weekly-review, chat, chat/confirm
  coach.module.ts
  coach.rules.spec.ts

apps/backend/src/ai/
  prompts/coach-phrasing.prompt.ts
  schemas/coach-phrasing.schema.ts
  features/coach-phrasing.feature.ts
  features/coach-phrasing.feature.spec.ts
  features/coaching-agent.feature.ts
```

`coach.rules.ts` has no framework, database or AI imports. All rule tests target it directly.
The AI files follow the established `prompts / schemas / features` pattern. `AiService` exposes
`phraseCoachMessage` for the proactive path and `coachChat` for the conversational one.

`CoachService` is the only writer to the coach chat. `PersonasService` keeps the agent logic and
knows nothing about chat.

## 5. API

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/coach/chat` | Send a message; both sides are stored in the coach chat |
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

`ChatService.postMessage`, `ChatService.assertParticipant`, `ChatGateway.emitToRoom`,
`HabitRepository.findByUserId`, `UserRepository.findAllUsers`, `PersonasService.driftCheck`,
`PersonasService.coachChat`, `PersonasService.confirmCoachChange`, `GeminiClient`,
`PROMPT_SAFETY_GUARDRAIL`.

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
