# HabitFlowAI (POC)

HabitFlowAI is a mobile + backend proof of concept for personalized habit coaching.
The backend uses Gemini to classify user motivation persona and generate goal plans, and the Android app consumes the API with Retrofit.

## What This Project Includes

- Backend: Node.js + TypeScript + Express + Swagger
- Frontend: Android app (Kotlin + Jetpack Compose)
- Auth flow: Register, login, JWT access token + refresh token
- AI flow: Persona classification, motivational message generation, and daily task variations

## Repository Structure

		Backend/
			src/
				config/
				controllers/
				dto/
				middleware/
				repository/
				routes/
				services/

		Frontend/
			app/src/main/

## Backend Setup

### Prerequisites

- Node.js 18+
- npm 9+

### Install dependencies

		cd Backend
		npm install

### Required local .env

Start from the template:

	copy Backend\\.env.example Backend\\.env

Or create a file at Backend/.env with:

		PORT=3000
		GEMINI_API_KEY=YOUR_GEMINI_API_KEY
		JWT_SECRET=YOUR_ACCESS_TOKEN_SECRET
		JWT_REFRESH_SECRET=YOUR_REFRESH_TOKEN_SECRET

Environment variables used by the backend:

- PORT: Optional. Defaults to 3000.
- GEMINI_API_KEY: Required. Used by Gemini SDK in AI service.
- JWT_SECRET: Required. Used to sign/verify access tokens.
- JWT_REFRESH_SECRET: Required. Used to sign/verify refresh tokens.

Security note:

- Never commit Backend/.env.
- Rotate secrets immediately if they are ever exposed.

### Run backend

		cd Backend
		npm start

Server starts on:

- http://localhost:3000
- Swagger UI: http://localhost:3000/api-docs

## Swagger Documentation

Swagger is configured with OpenAPI 3.0 and generated from route + DTO annotations.

- Source config: Backend/src/config/swagger.ts
- Scanned annotations: Backend/src/routes/*.ts and Backend/src/dto/*.ts
- Global auth scheme: Bearer JWT

How to use Swagger auth:

1. Login using the auth endpoint to get an access token.
2. Click Authorize in Swagger UI.
3. Paste: Bearer YOUR_ACCESS_TOKEN
4. Call protected endpoints.

## API Reference

Base URL:

- http://localhost:3000

### Auth

- POST /api/v1/auth/register
	- Public
	- Body: email, password, goal, quizAnswers[]
	- Response: userId, success

- POST /api/v1/auth/login
	- Public
	- Body: email, password
	- Response: accessToken, refreshToken, success

- POST /api/v1/auth/refresh
	- Public
	- Body: refreshToken
	- Response: accessToken, success

- POST /api/v1/auth/logout
	- Protected (Bearer token)
	- Response: success message

### Users

- GET /api/v1/users/me/home
	- Protected (Bearer token)
	- Returns: goal, personaType, motivationalMessage, coreGoals, dailyVariations
	- Daily variations are regenerated automatically once per day.

### Tasks

- PATCH /api/v1/tasks/:taskId/complete
	- Protected (Bearer token)
	- Marks task as completed.

### Personas

- POST /api/v1/personas/reclassify
	- Protected (Bearer token)
	- Body: goal, quizAnswers[]
	- Reclassifies persona and regenerates motivational message + tasks.

### Utility

- GET /api/v1/models
	- Public
	- Returns available Gemini models that support generateContent.

### Not currently mounted

- Route file exists for POST /api/v1/goals/generate, but the goals router is not mounted in Backend/src/server.ts right now.

## Data Persistence (Important for POC)

User data is currently stored in-memory (Map in repository layer).

Implications:

- Restarting backend clears all users/tasks/tokens.
- This is expected for the current POC phase.

## Frontend Setup (Android)

### Prerequisites

- Android Studio (recent stable)
- JDK 17
- Android SDK / Gradle setup required by project files

### Run

1. Open Frontend in Android Studio.
2. Sync Gradle.
3. Run app module on emulator/device.

### Backend URL in app

Current Retrofit base URL is defined in:

- Frontend/app/src/main/java/com/habitflowai/data/network/RetrofitProvider.kt

Current value:

		http://10.0.2.2:3000/

Notes:

- 10.0.2.2 is Android emulator alias to host localhost.
- For a physical device, use your machine LAN IP (for example http://192.168.1.25:3000/).
- Device and backend machine must be on the same network.

## Quick Local Validation Flow

1. Start backend.
2. Open Swagger at /api-docs.
3. Register a user.
4. Login and copy accessToken.
5. Authorize Swagger with Bearer token.
6. Call GET /api/v1/users/me/home.
7. Complete a task with PATCH /api/v1/tasks/:taskId/complete.

## Build Scripts

Backend package scripts:

- npm start: Run backend with ts-node
- npm run build: Compile TypeScript

## Current Scope Notes

- This repository contains legacy/parallel Android package paths under both com.habitflowai and com.example.habitflowai.
- The backend API in this README reflects currently mounted routes in server.ts.
