# HabitFlowAI — Project Context & Setup

## Google Maps / Geocoding Feature

Server-side Google Maps integration used to reverse-geocode user location check-ins into human-readable place names/addresses.

### Architecture

- **`apps/backend/src/locations/geocoding.service.ts`** — `GoogleGeocodingService`
  - Reads the `GOOGLE_MAPS_API_KEY` from config; returns `{}` if absent (graceful degradation, never throws).
  - `reverseGeocode(lat, lng)` calls the Google **Geocoding API**:
    `https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&language=en&key={key}`
  - Returns `{ placeName?, address? }` derived from `results[0].formatted_address`.
- **`apps/backend/src/locations/locations.service.ts`** — `LocationsService`
  - `recordLocation()` persists a location, then calls `reverseGeocode` and updates the stored place name.
  - `getMyLocations()`, `getBbox(minLat, maxLat, minLng, maxLng)` for map bounding-box queries.
- Module wiring: **`apps/backend/src/locations/locations.module.ts`** registers `GoogleGeocodingService`.
- E2E mocks set `process.env['GOOGLE_MAPS_API_KEY'] = 'test-maps-key'` in `apps/backend/src/e2e/jest.setup.ts`.
- Unit spec: **`apps/backend/src/locations/geocoding.service.spec.ts`**.

### Configuration

- **Env var:** `GOOGLE_MAPS_API_KEY` (required)
  - Must be declared in `apps/backend/.env` for local run.
  - Declared in placeholder file `apps/backend/.env.example` (value `your_google_maps_api_key`).
  - Enforced at boot by `apps/backend/src/app.module.ts`: `GOOGLE_MAPS_API_KEY: Joi.string().required()`.
- Recommendation from original setup: server-side key restricted by **IP** and limited to the **Geocoding API** only.

### Android side

- Map UI / route lives under `apps/android/app/src/main/java/com/habitflowai/presentation/ui/map/` (e.g. `MapRoute.kt`).
- `apps/android/app/src/main/res/xml/network_security_config.xml` controls cleartext-network policy for local/emulator dev.

### Gotchas

- Without a valid `GOOGLE_MAPS_API_KEY`, the backend still boots/tests fine because `reverseGeocode` bails out gracefully — so a missing/invalid key manifests as empty place names rather than hard failures.
- `.env` and `.env.example` contain secrets in this repo; `.env` is git-ignored (see `.gitignore` lines 4–7). Do not commit real keys.