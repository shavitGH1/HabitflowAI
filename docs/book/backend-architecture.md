# Backend Architecture

Author: Nir Shitrit (Member 1 — Backend / API / Auth / Infra)

This section describes the design of the HabitFlow AI backend: the NestJS API that
powers persona classification, habit tracking, the AI-driven motivation system, and
push notifications for the Android client.

## 1. Stack Overview

| Layer | Technology |
|---|---|
| Runtime | Node.js + TypeScript (strict mode) |
| Framework | NestJS (modular, dependency-injected) |
| Database | MongoDB, run locally via Docker (port 27018) |
| ODM | Mongoose |
| Monorepo tooling | pnpm workspaces + Turbo |
| Auth | JWT (access + refresh) via `jsonwebtoken`, bcrypt, Google OAuth 2.0 |
| AI | Google Gemini 2.5, accessed through a modular `src/ai/` layer |
| Push notifications | Firebase Admin SDK (Cloud Messaging) |
| API docs | Swagger / OpenAPI, served at `/api-docs` |
| Testing | Jest 29 (unit) + Supertest + `mongodb-memory-server` (E2E) |

The backend lives at `apps/backend` inside the monorepo, alongside the Android app
(`apps/android`) and shared TypeScript types (`packages/shared-types`). Turbo caches
build/test output per package so `pnpm build` and `pnpm test` from the repo root only
re-run what changed.

The application is bootstrapped in `src/main.ts`: a single `NestExpressApplication`
with a global `api/v1` prefix, a global `ValidationPipe` (whitelist + transform), static
file serving for `/uploads`, and Swagger mounted at `/api-docs`.

## 2. Module Structure

`AppModule` (`src/app.module.ts`) wires up cross-cutting infrastructure —
`ConfigModule` (with a Joi validation schema so the app refuses to boot with a missing
env var), `MongooseModule`, a global `ThrottlerModule` (5 requests / 60s), and
`ScheduleModule` for cron jobs — then imports eight feature modules:

| Module | Responsibility |
|---|---|
| `AuthModule` | Registration, login, refresh, logout, Google OAuth, FCM token registration |
| `UsersModule` | Read model for the authenticated user's home screen (persona, goals, portfolio) |
| `TasksModule` | Marking a generated daily task as complete |
| `PersonasModule` | Persona reclassification, weekly drift detection, and the drift cron job |
| `InsightsModule` | Weekly AI-generated insights and motivation-message feedback |
| `LocationsModule` | Recording and querying (bounding-box) habit-completion locations |
| `HabitsModule` | Habit CRUD, completion, and streak recalculation |
| `PostsModule` | Success-journal posts, with optional image upload |

Three supporting modules are not imported directly by `AppModule` but are pulled in by
the feature modules above, following Nest's dependency-injection graph rather than a
flat list:

- **`AiModule`** (`src/ai/`) — imported by `AuthModule` (persona classification at
  registration), `PersonasModule`, and `InsightsModule`.
- **`NotificationsModule`** (`src/notifications/`) — imported by `PersonasModule` for
  the drift-detection push pipeline (Section 5).
- **`StorageModule`** (`src/storage/`) — imported by `PostsModule` for image uploads
  (Section 6).

`DatabaseModule` provides shared Mongoose plumbing and is imported wherever a module
needs direct collection access. Every module follows the same three-layer shape:
`Controller → Service → Repository/Mongoose model`, matching the layered pattern
mandated by the project's coding standards.

## 3. Auth Flow

Auth is handled entirely in `AuthModule`/`AuthService`, without Nest's `JwtModule` —
tokens are signed and verified directly with `jsonwebtoken`, keeping the module small
and dependency-light.

- **Register** (`POST /auth/register`) — takes `email`, `password`, and the
  onboarding `openAnswers[]`. Persona classification happens synchronously during
  registration: `AiService.classifyPersonaWeighted` scores the answers against the
  Six Pillars model, and `AiService.generatePortfolio` produces the user's initial
  goals and motivational summary. The password is hashed with `bcrypt` (cost factor
  10) before the user document is persisted. The response returns `userId`,
  `personaType`, and the generated portfolio.
- **Login** (`POST /auth/login`) — verifies the password with `bcrypt.compare`,
  regenerates the day's task variations if they haven't been generated yet, then
  issues a signed access token (`JWT_SECRET`, 15 min) and refresh token
  (`JWT_REFRESH_SECRET`, 7 days). The refresh token is persisted on the user document
  so it can be checked and revoked later. Rate-limited via `ThrottlerGuard`.
- **Refresh** (`POST /auth/refresh`) — verifies the refresh token against
  `JWT_REFRESH_SECRET` and cross-checks it against the value stored on the user
  document (so a token can't be reused after logout), then issues a new access
  token.
- **Logout** (`POST /auth/logout`) — clears the stored refresh token, invalidating
  it immediately.
- **Google OAuth** (`GET /auth/google`, `GET /auth/google/callback`) — implemented
  via `passport-google-oauth20` (`GoogleStrategy`). This is a login-only flow: the
  callback looks up the Google account's email against existing users and issues the
  same access/refresh token pair as a normal login. It does not create new accounts,
  since persona classification depends on the onboarding questionnaire.

Protected routes use `JwtAuthGuard`, backed by `JwtStrategy` (`passport-jwt`), which
extracts the bearer token, verifies it against `JWT_SECRET`, and attaches
`{ id: string }` to the request as `req.user`.

## 4. AI Integration

AI features live under `src/ai/` and are designed to be purely additive — adding a
new feature means adding one file under `features/` and one prompt file, without
touching existing code:

```
src/ai/
├── gemini.client.ts        thin wrapper around the Gemini SDK
├── ai.service.ts           orchestration — picks the right feature per call
├── features/               one self-contained module per AI capability
│   ├── persona-classifier.feature.ts
│   ├── portfolio-generator.feature.ts
│   ├── daily-motivation.feature.ts
│   ├── persona-drift-detector.feature.ts
│   └── habit-insights.feature.ts
├── feedback/
│   └── motivation-feedback.store.ts
└── prompts/                 prompt text kept out of business logic
    ├── persona-classifier.prompt.ts
    ├── daily-motivation.prompt.ts
    ├── persona-drift-detector.prompt.ts
    └── habit-insights.prompt.ts
```

All Gemini calls go through `GeminiClient`, the single place where retries, timeouts,
and quota concerns are handled. `AiService` exposes typed methods
(`classifyPersonaWeighted`, `generatePortfolio`, `generateDailyVariations`,
`detectDrift`, `getWeeklyInsights`, `recordMotivationFeedback`) that `AuthModule`,
`PersonasModule`, and `InsightsModule` depend on — none of those consumers talk to
Gemini directly. AI-calling endpoints (`/personas/reclassify`, `/personas/drift-check`,
`/ai/motivation-feedback`) sit behind `ThrottlerGuard` to protect Gemini's free-tier
quota during testing and demos. Model output is validated before it reaches a
controller — persona type is checked against the known enum before being persisted or
returned to the client.

## 5. Drift Detection Pipeline

Persona drift detection answers a specific question: is a user's day-to-day behavior
still consistent with the persona they were classified into, or has it drifted toward
a different one? The pipeline runs both on demand (`POST /personas/drift-check`) and
on a schedule:

1. **`PersonaDriftScheduler`** (`src/personas/persona-drift.scheduler.ts`) runs every
   Monday at 9am (`@Cron('0 9 * * 1')`) and calls
   `PersonasService.evaluateAllUsersDrift()`.
2. For each user, `PersonasService.evaluateDrift` builds a `BehaviorSnapshot` and asks
   `AiService.detectDrift` (Gemini) whether the snapshot still matches the user's
   current persona.
3. If `driftDetected` is true, a `DriftFlag` document is persisted via
   `DriftFlagRepository` (`userId`, `detectedAt`, `driftScore`, `suggestedPersona`),
   giving the Android client a durable record to show a drift banner from, even if the
   push notification is missed.
4. If the user has a registered `fcmToken`, `PersonasService` sends a push
   notification through `FIREBASE_MESSAGING` (provided by `FirebaseModule`), prompting
   the user to review their persona in-app.

`FirebaseModule` initializes the Firebase Admin SDK once (guarded by
`getApps().length`) from three env vars — `FIREBASE_PROJECT_ID`,
`FIREBASE_PRIVATE_KEY`, `FIREBASE_CLIENT_EMAIL` — reconstructing the PEM private key
by replacing the literal `\n` sequences that survive `.env` parsing with real
newlines before it's handed to `cert()`.

## 6. Storage Adapter Pattern

Image uploads (currently only for `PostsModule`'s success-journal posts) go through an
`IStorageAdapter` interface (`src/storage/storage.adapter.ts`):

```ts
export interface IStorageAdapter {
  upload(file: Express.Multer.File): Promise<string>;
  delete(url: string): Promise<void>;
}
```

`StorageModule` binds this interface to `LocalStorageAdapter` via Nest's
`STORAGE_ADAPTER` injection token. `LocalStorageAdapter` writes files to an
`uploads/` directory (served statically by `main.ts`) under a UUID filename, and
`PostsService`/`PostRepository` only ever depend on the `IStorageAdapter` interface —
never on the local filesystem directly. Swapping to a cloud provider (S3, GCS,
Firebase Storage) for production is a matter of adding a new class that implements
`IStorageAdapter` and changing the provider binding in `StorageModule`; no other module
needs to change.

## 7. Testing Strategy

The backend has 9 spec suites (28 unit tests + 14 E2E tests, all passing):

**Unit tests (Jest)** cover the service layer for core, non-trivial logic: auth
(`auth.service.spec.ts`), habit CRUD and streak calculation
(`habits.service.spec.ts`, `streak.utils.spec.ts`), and three AI features
(`daily-motivation`, `habit-insights`, `persona-drift-detector`). Gemini and Mongoose
are mocked at these boundaries — unit tests exercise business logic, not third-party
integrations.

**E2E tests** (`src/e2e/`) run the real Nest module graph against an in-memory
MongoDB instance via `mongodb-memory-server`, using Supertest to drive real HTTP
requests through the app's global prefix and validation pipe. `createTestApp()`
(`app.helper.ts`) overrides only the pieces that must not touch external services in
CI — `AiService` (mocked Gemini responses), `FIREBASE_MESSAGING` (mocked `send`), and
`ThrottlerGuard` (disabled, so tests aren't rate-limited) — while everything else,
including the real Mongoose schemas and repositories, runs unmodified. Three journeys
are covered end-to-end:

- **Auth flow** (`auth.e2e.spec.ts`): register → login → refresh → logout.
- **Habit CRUD** (`habits.e2e.spec.ts`): create → list → update → complete (streak
  increments) → delete (soft delete, no longer listed).
- **Persona classification** (`persona.e2e.spec.ts`): register with a full set of
  onboarding answers, assert the returned `personaType` is one of the six known
  personas and `coreGoals[]` is non-empty.

## Appendix: Swagger

All 25 endpoints across 8 tags (`auth`, `users`, `tasks`, `personas`, `ai`,
`locations`, `habits`, `posts`) are documented with `@ApiTags`, `@ApiOperation`, and
`@ApiResponse` decorators, and are browsable at `/api-docs`. A full export of the
OpenAPI document is included alongside this section at
[`appendix/swagger.json`](appendix/swagger.json), generated from a live run of the
backend (`GET /api-docs-json`).
