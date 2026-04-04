# HabitFlowAI - POC

HabitFlowAI is a proof-of-concept mobile + backend app that classifies a user into a motivation persona (`Architect` or `Achiever`) using Gemini, then returns a personalized motivational message.

## Project Structure

```text
Backend/   -> Node.js + TypeScript + Express API
Frontend/  -> Android app (Kotlin + Jetpack Compose)
```

## How The App Communicates With The Backend

The Android app uses Retrofit to call the backend endpoint:

- `POST /api/v1/personas/classify`

The base URL is currently hardcoded in:

- `Frontend/app/src/main/java/com/habitflowai/data/network/RetrofitProvider.kt`

Current value:

```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```

Important notes:

- `10.0.2.2` is a special alias from the Android emulator to your host machine's localhost.
- If running on a physical Android device, replace `10.0.2.2` with your computer's LAN IP (for example: `http://192.168.1.25:3000/`).
- Your phone and backend machine must be on the same network.
- Ensure your firewall allows inbound traffic to port `3000`.

## Backend Setup

### 1. Install dependencies

```bash
cd Backend
npm install
```

### 2. Create `.env` file

Create `Backend/.env` with:

```env
PORT=3000
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

Notes:

- `GEMINI_API_KEY` is required by `Backend/src/services/aiService.ts`.
- `PORT` is optional (defaults to `3000` if omitted).

### 3. Get a Gemini API key

1. Open Google AI Studio: https://aistudio.google.com/
2. Sign in with your Google account.
3. Go to **Get API key** (or **API keys** in the left menu).
4. Create a new key.
5. Copy the key and paste it into `Backend/.env` as `GEMINI_API_KEY`.

Security recommendation:

- Never commit `.env` files or API keys.
- If a key was ever committed, rotate/regenerate it immediately.

### 4. Start backend

```bash
cd Backend
npm start
```

When running, backend is available at:

- `http://localhost:3000`
- Swagger docs: `http://localhost:3000/api-docs`

## Frontend Setup (Android)

1. Open the `Frontend` folder in Android Studio.
2. Sync Gradle.
3. Run the app on an emulator or device.

Requirements:

- Android Studio Hedgehog+
- JDK 17
- Android SDK 34

## Quick End-to-End Test

1. Start backend (`npm start` in `Backend`).
2. Confirm `BASE_URL` in `RetrofitProvider.kt` points to your backend:
	- Emulator: `http://10.0.2.2:3000/`
	- Physical device: `http://<your-lan-ip>:3000/`
3. Launch Android app and finish onboarding.
4. Verify backend receives `POST /api/v1/personas/classify` and returns persona + message.
