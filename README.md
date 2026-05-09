# HabitFlow AI

HabitFlow AI is a smart Android habit coaching app that identifies a user's motivational
persona (Achiever, Grower, Socializer, Explorer, Altruist, Architect) using Gemini AI and
adapts the experience — goals, daily tasks, and motivation messages — to that persona.

## Stack

- **Monorepo:** pnpm workspaces + Turbo
- **Backend:** NestJS + TypeScript (strict), MongoDB via Docker, Mongoose
- **Auth:** JWT access token (15 min) + refresh token (7 days) + Google OAuth
- **AI:** Google Gemini (via AI Studio) — persona classification, portfolio generation, daily motivation
- **Android:** Kotlin + Jetpack Compose, MVVM + Clean Architecture, Room (offline SSOT), Hilt, Retrofit
- **Containerization:** Docker Compose (mongo + backend + mongo-express for dev)

## Repository Structure

```
/
├── apps/
│   ├── backend/          NestJS API
│   └── android/          Android app (Kotlin + Jetpack Compose)
├── packages/
│   └── shared-types/     Shared TypeScript enums (PersonaType, etc.)
├── docker-compose.yml
├── turbo.json
├── pnpm-workspace.yaml
└── package.json          pnpm workspace root
```

## Prerequisites

- Node.js 18+
- pnpm 9+ (`npm install -g pnpm`)
- Docker Desktop (running)
- Android Studio (for the Android app)
- JDK 17

---

## Backend Setup

### 1. Install dependencies

From the repo root:

```
pnpm install
```

### 2. Configure environment

```
copy apps\backend\.env.example apps\backend\.env
```

Edit `apps/backend/.env`:

```
PORT=3000

MONGO_URI=mongodb://admin:password@localhost:27017/habitflow?authSource=admin
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=password

GEMINI_API_KEY=your_gemini_api_key
JWT_SECRET=your_access_token_secret
JWT_REFRESH_SECRET=your_refresh_token_secret
```

Variable reference:

| Variable | Required | Description |
|---|---|---|
| PORT | No (default 3000) | Backend listen port |
| MONGO_URI | Yes | Full MongoDB connection string |
| MONGO_INITDB_ROOT_USERNAME | Yes | Mongo root user (matches docker-compose) |
| MONGO_INITDB_ROOT_PASSWORD | Yes | Mongo root password (matches docker-compose) |
| GEMINI_API_KEY | Yes | Google AI Studio key |
| JWT_SECRET | Yes | Signs access tokens |
| JWT_REFRESH_SECRET | Yes | Signs refresh tokens |

Never commit `.env`. Rotate secrets immediately if exposed.

### 3. Start the full stack (backend + MongoDB)

```
docker compose up --build
```

Or in detached mode:

```
docker compose up -d --build
```

Services started:

| Service | URL |
|---|---|
| Backend API | http://localhost:3000 |
| Swagger UI | http://localhost:3000/api-docs |
| mongo-express (dev) | http://localhost:8081 |

To start only MongoDB (run backend locally with hot reload):

```
docker compose up mongo -d
cd apps/backend
pnpm dev
```

### 4. Run backend tests

```
cd apps/backend
pnpm test
```

Or from root via Turbo:

```
turbo run test --filter=backend
```

---

## Android Setup

### 1. Open in Android Studio

Open the `apps/android/` folder in Android Studio. Sync Gradle when prompted.

### 2. Backend URL

The Retrofit base URL is set in:

```
apps/android/app/src/main/java/com/habitflowai/data/network/NetworkModule.kt
```

| Environment | URL |
|---|---|
| Android Emulator | http://10.0.2.2:3000/ |
| Physical Device (same LAN) | http://192.168.x.x:3000/ |

### 3. Run

Select a device/emulator and press Run.

---

## Monorepo Commands

Run from the repo root. Turbo caches results — repeated runs only re-execute what changed.

| Command | Description |
|---|---|
| `pnpm install` | Install all workspace dependencies |
| `turbo run build` | Build all apps |
| `turbo run test` | Run all test suites |
| `turbo run lint` | Lint all apps |
| `turbo run dev` | Start all apps in watch/dev mode |
| `turbo run build --filter=backend` | Build backend only |

---

## API Reference

Base URL: `http://localhost:3000`
Full interactive docs: `http://localhost:3000/api-docs`

### Auth

| Method | Endpoint | Auth | Body |
|---|---|---|---|
| POST | /api/v1/auth/register | Public | email, password, goal, openAnswers[] |
| POST | /api/v1/auth/login | Public | email, password |
| POST | /api/v1/auth/refresh | Public | refreshToken |
| POST | /api/v1/auth/logout | Bearer | — |
| GET | /api/v1/auth/google | Public | — (redirect) |

### Users

| Method | Endpoint | Auth | Returns |
|---|---|---|---|
| GET | /api/v1/users/me/home | Bearer | goal, personaType, portfolio, coreGoals, dailyVariations |

### Tasks

| Method | Endpoint | Auth | Body |
|---|---|---|---|
| PATCH | /api/v1/tasks/:taskId/complete | Bearer | — |

### Personas

| Method | Endpoint | Auth | Body |
|---|---|---|---|
| POST | /api/v1/personas/reclassify | Bearer | goal, openAnswers[] |
| POST | /api/v1/personas/drift-check | Bearer | — (server-side evaluation) |

### AI

| Method | Endpoint | Auth | Returns |
|---|---|---|---|
| GET | /api/v1/ai/weekly-insights | Bearer | AI-generated weekly summary |
| POST | /api/v1/ai/motivation-feedback | Bearer | thumbsUp: boolean |

---

## Swagger Auth

1. Login via `POST /api/v1/auth/login` and copy the `accessToken`.
2. Click **Authorize** in Swagger UI.
3. Enter: `Bearer YOUR_ACCESS_TOKEN`
4. Call any protected endpoint.

---

## Data Persistence

User data, habits, posts, and locations are stored in MongoDB. Data persists across
backend restarts. The mongo Docker volume is named `mongo_data` and lives outside
the container — removing the container does not delete data.

To wipe the database (dev only):

```
docker compose down -v
```

---

## Security Notes

- Passwords hashed with bcrypt (cost ≥ 10). Never logged.
- Refresh tokens stored hashed in MongoDB; invalidated on logout.
- All Gemini API keys live in the backend `.env` only — the Android app never holds them.
- Rate limiting applied on AI endpoints to protect Gemini free-tier quota.
