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
- **Containerization:** Docker Compose (MongoDB only — backend runs locally)

## Repository Structure

```
/
├── apps/
│   ├── backend/          NestJS API (runs locally)
│   └── android/          Android app (Kotlin + Jetpack Compose)
├── packages/
│   └── shared-types/     Shared TypeScript enums (PersonaType, etc.)
├── docker-compose.yml    MongoDB + mongo-express only
├── turbo.json
├── pnpm-workspace.yaml
└── package.json          pnpm workspace root
```

## Prerequisites

- Node.js 20+
- pnpm 9+ (`npm install -g pnpm`)
- Docker Desktop (for MongoDB)
- Android Studio (for the Android app)
- JDK 17

---

## Quick Start

```
pnpm install
docker compose up -d
cd apps/backend && pnpm dev
```

That's it. Backend is live at `http://localhost:3000`.

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
MONGO_URI=mongodb://admin:password@localhost:27018/habitflow?authSource=admin
GEMINI_API_KEY=your_gemini_api_key
JWT_SECRET=your_jwt_secret
JWT_REFRESH_SECRET=your_jwt_refresh_secret
```

Also create the root `.env` for Docker Compose:

```
copy .env.example .env
```

Variable reference:

| Variable | File | Required | Description |
|---|---|---|---|
| PORT | apps/backend/.env | No (default 3000) | Backend listen port |
| MONGO_URI | apps/backend/.env | Yes | MongoDB connection string |
| GEMINI_API_KEY | apps/backend/.env | Yes | Google AI Studio key |
| GOOGLE_MAPS_API_KEY | apps/backend/.env | Yes | Server-side Maps key (Geocoding API, IP-restricted) |
| JWT_SECRET | apps/backend/.env | Yes | Signs access tokens |
| JWT_REFRESH_SECRET | apps/backend/.env | Yes | Signs refresh tokens |
| MONGO_INITDB_ROOT_USERNAME | .env (root) | Yes | Mongo root user for Docker |
| MONGO_INITDB_ROOT_PASSWORD | .env (root) | Yes | Mongo root password for Docker |

Never commit either `.env` file. Rotate secrets immediately if exposed.

### 3. Start MongoDB

```
docker compose up -d
```

MongoDB is ready when `docker compose ps` shows `healthy` for `habitflow-mongo`.

### 4. Start the backend

```
cd apps/backend
pnpm dev
```

Or from the repo root:

```
pnpm dev
```

Services:

| Service | URL |
|---|---|
| Backend API | http://localhost:3000 |
| Swagger UI | http://localhost:3000/api-docs |

### 5. Start mongo-express (optional, GUI for MongoDB)

```
docker compose --profile dev up -d
```

mongo-express: http://localhost:8081

### 6. Run tests

```
pnpm test
```

Or from the backend directory:

```
cd apps/backend && pnpm test
```

### Stop MongoDB

```
docker compose down
```

To also wipe all data:

```
docker compose down -v
```

---

## Android Setup

### 1. Open in Android Studio

Open the `apps/android/` folder in Android Studio. Sync Gradle when prompted.

### 2. Set backend URL

Open `apps/android/local.properties` and set your backend URL:

```properties
# Emulator
backend.base.url=http://10.0.2.2:3000/

# Physical device (find your LAN IP via ipconfig)
backend.base.url=http://192.168.x.x:3000/
```

The default is `10.0.2.2:3000` (emulator) if the property is not set.

### 3. Run

Select a device/emulator and press Run.

---

## Monorepo Commands

Run from the repo root. Turbo caches results — repeated runs only re-execute what changed.

| Command | Description |
|---|---|
| `pnpm install` | Install all workspace dependencies |
| `pnpm dev` | Start backend in watch/dev mode |
| `pnpm build` | Build all apps |
| `pnpm test` | Run all test suites |
| `docker compose up -d` | Start MongoDB |
| `docker compose down` | Stop MongoDB |
| `docker compose down -v` | Stop MongoDB and wipe data |

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

### Users

| Method | Endpoint | Auth | Returns |
|---|---|---|---|
| GET | /api/v1/users/me/home | Bearer | goal, personaType, portfolio, coreGoals, dailyVariations |

### Tasks

| Method | Endpoint | Auth | Body |
|---|---|---|---|
| PATCH | /api/v1/tasks/:taskId/complete | Bearer | — |

### Locations

| Method | Endpoint | Auth | Body / Query |
|---|---|---|---|
| POST | /api/v1/locations | Bearer | habitId, taskTitle, latitude, longitude, timestamp?, isPublic? |
| GET | /api/v1/locations/me | Bearer | — (returns your completion locations, newest first) |

### Personas

| Method | Endpoint | Auth | Body |
|---|---|---|---|
| POST | /api/v1/personas/reclassify | Bearer | goal, openAnswers[] |

---

## Swagger Auth

1. Login via `POST /api/v1/auth/login` and copy the `accessToken`.
2. Click **Authorize** in Swagger UI.
3. Enter: `Bearer YOUR_ACCESS_TOKEN`
4. Call any protected endpoint.

---

## Data Persistence

User data is stored in MongoDB running in Docker. Data persists across restarts via the
`mongo_data` Docker volume. Removing the container does **not** delete data.

---

## Security Notes

- Passwords hashed with bcrypt (cost ≥ 10). Never logged.
- Refresh tokens stored in MongoDB; invalidated on logout.
- All Gemini API keys live in `apps/backend/.env` only — the Android app never holds them.
