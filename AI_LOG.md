# AI Usage Log

This prototype was built with AI assistance across three phases: design brainstorming, plan generation, and implementation. Key prompts are summarized below.

## Phase 1 — Design brainstorming

Prompt themes used to shape the design before any code:

- "Design a local meeting planner: users sign up/log in, create meetings with participants, view meeting details, upload an avatar. What is the minimal architecture?"
- "Compare storing JWT in localStorage vs cookies for a local prototype; pick the simplest secure-enough option." → Chose `localStorage` + `Bearer` header + interceptor.
- "How should participants be modeled — as registered users or free-form entries?" → Chose free-form `{ name, email? }` to avoid an invite system.
- "Choose a datastore that persists locally with zero external setup." → H2 in file mode.
- "How to handle avatar uploads locally?" → Local disk storage under `uploads/`, PNG/JPEG only, served via a file endpoint.
- "Should recurrence generate occurrences?" → No; recurrence kept as a descriptive label for the prototype.

## Phase 2 — Plan generation

- "Produce a task-by-task implementation plan with full code for every file: Spring Boot backend (auth, meetings, files, security) and Angular standalone frontend (services, guard, interceptor, 5 pages), including unit/integration tests. No comments in source. Use TDD where it adds value (JWT service, auth service, meeting service, file storage, controller integration)."
- Output: `docs/superpowers/plans/2026-07-15-meeting-planner.md` — 23 tasks, each with checkbox steps, exact code, and expected build/test results.

## Phase 3 — Implementation

- Backend tasks (1–12): scaffold, user entity, JWT service (TDD), security config + filter, auth service/controller (TDD), exception handler, meeting domain, meeting DTOs, meeting service (TDD), controllers, file storage + avatar endpoints (TDD), MockMvc integration tests.
- Frontend tasks (13–21), executed by a subagent from the plan verbatim:
  - "Scaffold Angular 17 standalone workspace with routing, wire `proxy.conf.json` into `angular.json`."
  - "Implement core `models.ts`, `auth.service.ts`, `api.service.ts`, `auth.interceptor.ts`, `auth.guard.ts`, and `app.config.ts`."
  - "Implement routes, app shell, and the five pages: login, meetings-list, meeting-form, meeting-detail, profile."
  - "Add frontend specs (auth.service, auth.guard, login.component); verify `ng build` and headless `ng test` pass."
- Task 22: "Write README (prerequisites, run/test instructions, design decisions, assumptions, limitations, future work) and this AI log."

## Guardrails applied throughout

- Hard rule: no comments in any source file.
- Prefer editing generated scaffold files over adding new structure.
- Every code block in the plan was used as-is unless it failed to compile/build.
