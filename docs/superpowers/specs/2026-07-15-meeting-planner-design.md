# Meeting Planner Prototype — Design

Date: 2026-07-15
Status: Approved

## Context

Take-home coding exercise for lead developers. Build a local meeting planner
prototype. Goal is to demonstrate design decisions and communication, not a
production product. Timebox 2-3 hours.

Mandatory stack: Java + Spring Boot backend, H2 database, Angular frontend.
Local only, local filesystem for uploads, no cloud/external auth/deployment.
Must be easy to run from a fresh checkout.

## Locked Decisions

- Auth: JWT (stateless), Spring Security, BCrypt password hashing.
- Participants: free-form entries (name + email) added to a meeting by its
  owner. No account required to be a participant.
- Meeting fields: title (required), description, location, startTime, endTime
  (end > start enforced), timezone (IANA string), recurrence enum
  (NONE | DAILY | WEEKLY | MONTHLY, label only), status enum
  (SCHEDULED | CANCELLED | COMPLETED), owner, participants, createdAt.
- No code comments anywhere (explicit user requirement).

## Repository Layout

```
meeting-planner/
  backend/    Spring Boot (Java 17, Maven)
  frontend/   Angular (standalone components)
  README.md
  AI_LOG.md
```

Backend REST API on :8080. Angular dev server on :4200 with proxy to 8080.
H2 in file mode (persists across restarts, zero setup). Avatars stored in
local ./uploads/avatars/.

## Backend

Layers: Controller -> Service -> Repository (Spring Data JPA) -> H2.

### Entities

- User: id, email (unique), passwordHash (BCrypt), displayName, avatarPath,
  createdAt.
- Meeting: id, title, description, location, startTime, endTime, timezone,
  recurrence (enum), status (enum), owner (FK User), createdAt.
- Participant: id, meeting (FK), name, email.

### Auth

Spring Security + JWT. Signup and login are public. All other /api/** require
Authorization: Bearer <token>. Passwords BCrypt-hashed. JWT signed HS256,
secret in application.yml.

### Endpoints

```
POST  /api/auth/signup           {email,password,displayName}  -> {token}
POST  /api/auth/login            {email,password}              -> {token}
GET   /api/me                                                  -> current user
POST  /api/me/avatar             multipart file                -> {avatarUrl}
GET   /api/meetings                                            -> owner meetings
POST  /api/meetings              {title,...,participants[]}     -> created meeting
GET   /api/meetings/{id}                                       -> detail
PATCH /api/meetings/{id}/status  {status}                      -> updated
GET   /api/files/avatars/{name}                                -> image bytes
```

Ownership enforced: a user may only read/edit own meetings, else 403.
Detail response includes computed duration and participantCount.

### Validation / Errors

Bean Validation (@NotBlank, @Email). Custom end > start check.
@RestControllerAdvice maps errors to JSON {status,message,fieldErrors}.
400 validation, 401 bad credentials, 403 ownership, 404 missing.

### Avatar Upload

Validate content-type (png/jpeg) and size cap (2MB). Random filename. Store in
./uploads/avatars/. Save path on user record.

## Frontend (Angular)

Standalone components, Angular Router, HttpClient.

```
src/app/
  core/
    auth.service.ts        login/signup/logout, token in localStorage
    auth.interceptor.ts    attach Bearer token
    auth.guard.ts          protect routes
    api.service.ts         meeting + user HTTP calls
    models.ts              User, Meeting, Participant interfaces
  pages/
    login/                 login + signup toggle
    meetings-list/         owner meetings table + New button
    meeting-form/          create meeting, dynamic participant rows
    meeting-detail/        full detail (duration, status, participants)
    profile/               user info + avatar upload
  app.routes.ts
```

Routes: /login public. /meetings, /meetings/new, /meetings/:id, /profile
guarded. Default redirect to /meetings, unauth to /login.

Flow: login -> store JWT -> interceptor adds header -> guard gates pages.
401 response -> clear token, redirect to login.

Reactive forms. Participant rows dynamically add/remove. proxy.conf.json maps
/api to localhost:8080 (no CORS config needed in dev). Minimal CSS, no UI
library.

## Testing

Backend (JUnit 5 + Spring Boot Test) — priority:
- MeetingService: end-before-start rejected; duration computed; ownership
  enforced (other user denied).
- AuthService: signup hashes password; duplicate email rejected; login wrong
  password fails; JWT generate -> validate round-trip.
- Controller slice (MockMvc): create meeting + fetch detail; unauth -> 401.
- Avatar: reject bad content-type / oversize.

Frontend (Jasmine/Karma) — light:
- AuthService stores/clears token.
- auth.guard redirects when no token.
- one component render smoke test.

Edge cases: invalid time range, duplicate signup, missing auth, bad file
upload, cross-user access.

## Deliverables

- Multiple commits: scaffold -> entities -> auth -> meetings -> avatar ->
  frontend -> tests -> README.
- README: run steps, test commands, design decisions, assumptions, known
  limitations, future improvements.
- AI_LOG.md: key prompts.

## Known Limitations (prototype)

- Recurrence is a label only; no occurrence expansion.
- No participant notifications / email.
- JWT secret and H2 credentials in config (fine for local-only).
- No refresh tokens; single access token in localStorage.
