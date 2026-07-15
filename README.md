# Meeting Planner Prototype

A local meeting planner with JWT authentication, avatar upload, and meeting CRUD + detail views. Spring Boot + H2 backend, Angular standalone frontend.

## Architecture

Monorepo:

- `backend/` — Spring Boot REST API (layered Controller -> Service -> Repository over H2, file mode). Runs on `:8080`.
- `frontend/` — Angular 17 standalone components. JWT stored in `localStorage`, attached via HTTP interceptor, routes protected by a guard. Dev server runs on `:4200` and proxies `/api` to the backend.

## Prerequisites

- Java 17 (expected at `C:\Program Files\Java\jdk-17`)
- Node.js 18+ (developed on v20.19.0) and npm (10.8.2)
- Maven wrapper (`mvnw`) is included in `backend/`; no global Maven required.

## Running the backend

```bash
cd backend
# Windows (PowerShell):
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
./mvnw spring-boot:run
```

The API starts on http://localhost:8080. H2 runs in file mode (data persisted under `backend/data/`). The H2 console is available at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/planner`, user `sa`, empty password). Uploaded avatars are written to `backend/uploads/avatars/`.

## Running the frontend

```bash
cd frontend
npm install
npm start
```

Open http://localhost:4200. The dev server proxies `/api` calls to the backend on `:8080` via `proxy.conf.json`, so start the backend first.

## Running the tests

Backend (JUnit 5 + MockMvc):

```bash
cd backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
./mvnw test
```

Frontend (Jasmine/Karma, headless):

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless
```

Requires Chrome/Chromium installed for the headless runner.

## Design decisions

- **JWT auth** — Stateless authentication. Backend signs an HS256 token on signup/login; the frontend stores it in `localStorage` and sends it as a `Bearer` header. A `OncePerRequestFilter` resolves the token to the current `User`, exposed to controllers via `@AuthenticationPrincipal`.
- **Free-form participants** — Participants are plain `{ name, email? }` records tied to a meeting, not references to registered users. Keeps the prototype simple and avoids an invite/RSVP system.
- **H2 file mode** — Data persists between restarts without needing an external database. `ddl-auto: update` auto-manages the schema.
- **Local avatar storage** — Uploaded images are stored on disk under `uploads/avatars/` with a UUID filename; only PNG/JPEG allowed, 2MB cap. Served back through `/api/files/avatars/{filename}`.
- **Recurrence as a label** — Recurrence (`NONE/DAILY/WEEKLY/MONTHLY`) is a descriptive field only; the app does not expand recurring meetings into multiple instances.
- **Ownership isolation** — A user can only list, view, and mutate meetings they own; cross-user access returns 403.

## Assumptions

- Single-tenant, local/prototype use; no email delivery, no calendar sync.
- Timezone is stored as an IANA string supplied by the client; times are stored as wall-clock `LocalDateTime` plus that timezone label.
- No password strength policy beyond "not blank".
- Meeting duration and participant count are derived server-side for display.

## Known limitations

- No refresh tokens; a single long-lived (24h) JWT. No server-side logout/revocation.
- No editing of meetings after creation (only status can change); no delete endpoint.
- Recurrence does not generate occurrences.
- Avatar files are not garbage-collected when replaced.
- H2 `ddl-auto: update` is convenient but not a substitute for real migrations.

## Future improvements

- Meeting edit/delete, and recurrence expansion into concrete occurrences.
- Participant invitations tied to registered users, with RSVP status.
- Refresh-token rotation and token revocation.
- Pagination/filtering/search on the meetings list.
- Flyway/Liquibase migrations and a production-grade database.
- Server-side timezone conversion and conflict detection.
