# Standbase — Backend

The REST API for [Standbase](https://standbase.shivankkapoor.com), a personal standup journal. Built with Spring Boot 4 and Java 25, backed by PostgreSQL, and deployed via Cloudflare Tunnels.

> **Frontend repo:** [standbase-frontend](https://github.com/ShivankKapoor/standbase-frontend)

## Tech Stack

- **Java 25** / **Spring Boot 4.0** (Gradle, Kotlin DSL)
- **PostgreSQL** — with `tsvector` full-text search index on entry content (maintained by a trigger)
- **Spring Security** — stateless auth via a custom filter that delegates every login, TOTP
  verification, and session check to [Aldrop](https://github.com/ShivankKapoor/aldrop), a
  standalone auth microservice — see [Auth Flow](#auth-flow)
- **Bucket4j + Caffeine** — in-memory rate limiting (see [Rate Limiting](#rate-limiting))
- **Lombok** — boilerplate reduction
- **Cloudflare Tunnels** — public exposure; client IP is read from `CF-Connecting-IP`, falling back
  to `X-Forwarded-For`, `X-Real-IP`, `X-Client-IP`, then the remote address (loopback and private
  addresses in headers are ignored)
- **Discord Webhooks** — login, TOTP, and logout notifications (optional)

## Features

- Two-step login: password → TOTP (if enabled), each step issuing a short-lived token, both
  handled by Aldrop
- Sessions are device-bound (IP + User-Agent) by Aldrop — every request is validated against it,
  so a revoked or expired session stops working immediately
- Daily entries (one per user per date) with an optional day type: `PTO`, `PLANNING`, or `SUPPORT`
- Per-date todos with completed/cancelled states and manual ordering
- Word-count history for the stats heatmap (past year)
- Word count stored as a generated column in PostgreSQL
- Rate limiting on login/TOTP, invalid session tokens, and entry/session/heatmap requests

## Prerequisites

- Java 25+
- PostgreSQL (run `schema.sql` to initialise tables, indexes, and the `standbase_app` role)
- A running [Aldrop](https://github.com/ShivankKapoor/aldrop) instance with a platform registered
  for Standbase (see that repo's README for platform setup)

## Setup

### 1. Database

Edit the placeholder password in `schema.sql` (`CREATE USER standbase_app ...`), then run it
against your PostgreSQL instance as a superuser:

```bash
psql -U postgres -d standbase -f schema.sql
```

This creates the `users`, `entries`, and `todos` tables, all indexes, the `tsvector` trigger, and
the least-privilege `standbase_app` role (CRUD only; no schema changes). Hibernate runs with
`ddl-auto=validate`, so the schema must already exist.

### 2. Environment

Create a `.env` file in the project root (see `example.env`):

```env
ENV=DEV
DB_URL=jdbc:postgresql://<host>:<port>/<dbname>
DB_USER=standbase_app
DB_PASSWORD=<your_password>
SERVER_PORT=5554
CORS_ALLOWED_ORIGINS=http://localhost:5173
DISCORD_WEBHOOK=          # optional
ALDROP_BASE_URL=          # Aldrop instance base URL
ALDROP_API_KEY=           # API key from the Aldrop platform registered for Standbase
```

| Variable | Required | Default | Notes |
|----------|----------|---------|-------|
| `DB_USER`, `DB_PASSWORD` | yes | — | |
| `ALDROP_BASE_URL`, `ALDROP_API_KEY` | yes | — | |
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/standbase` | |
| `SERVER_PORT` | no | `5554` | |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5555` | Comma-separated list |
| `DISCORD_WEBHOOK` | no | empty (disabled) | |
| `ENV` | no | `UNKNOWN` | Reported by `/monitor` |

### 3. Create a User

There is no local registration endpoint — user accounts live entirely in Aldrop. Register the
user against Aldrop directly (`POST /auth/register` on the Standbase platform), using the same
id as any existing Standbase `users` row if you're migrating one. Standbase provisions its own
local `users` row automatically the first time that Aldrop user authenticates.

### 4. Run

```bash
./gradlew bootRun
```

Run the tests with `./gradlew test` (also run by GitHub Actions on pushes and PRs to `main`).

### Docker

```bash
docker compose up -d --build
```

The compose file builds the image, loads `.env`, publishes port `5554`, and restarts the
container automatically. The `.env` file is also mounted read-only into `/deployments`. Keep
`SERVER_PORT=5554` so the published port matches.

## API

Errors from validation or malformed requests return `400` with a JSON body; unexpected errors
return `500` with `{ "error": ... }`.

### Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | HTML welcome page |
| `GET` | `/health` | Returns `{ app, db }` — `Up`/`Down` status of the app and database |
| `GET` | `/monitor` | Returns name, status, env, uptime, and Java runtime details |
| `POST` | `/auth/login` | Step 1 of login — `{ username, password }`. Returns `{ status: "ok", sessionToken }` or `{ status: "totp_required", preAuthToken }`. `401` on bad credentials, `429` when rate-limited |
| `POST` | `/auth/totp/verify` | Step 2 of login — `{ preAuthToken, totpCode }`. Returns `{ status: "ok", sessionToken }`. `401` / `429` as above |

### Authenticated

All endpoints below require `Authorization: Bearer <token>`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/session/check` | Validate session — returns `{ status, userName }` |
| `POST` | `/session/logout` | Revoke the session in Aldrop |
| `GET` | `/entry?year=&month=` | List `{ date, dayType }` for every entry in a month |
| `GET` | `/entry/{date}` | Get a single entry by date (`YYYY-MM-DD`); `404` if none |
| `POST` | `/entry` | Create or update the entry for `date` — `{ date, content, dayType }` |
| `DELETE` | `/entry/{date}` | Delete an entry by date (`204`, or `404` if none) |
| `GET` | `/heatmap?today=` | Word counts for each entry in the year up to `today` (`YYYY-MM-DD`), plus the average words per entry |
| `GET` | `/todos?date=` | List todos for a date, ordered by position |
| `GET` | `/todos/summary?year=&month=` | Per date in the month that has todos: `{ date, allCompleted }` (completed or cancelled counts as done) |
| `POST` | `/todos` | Create a todo — `{ entryDate, content }`; appended at the end of that date's list |
| `PUT` | `/todos/{id}` | Partial update of `content`, `completed`, `cancelled`, or `entryDate` (changing the date moves the todo to the end of the new date's list) |
| `PUT` | `/todos/reorder` | Reorder todos — `{ ids: [...] }` in the desired order |
| `DELETE` | `/todos/{id}` | Delete a todo (`204`, or `404` if not yours / missing) |

Entries and todos are always scoped to the authenticated user.

## Auth Flow

Standbase holds no passwords, TOTP secrets, or session state of its own — every step below is a
call through to Aldrop.

1. `POST /auth/login` with `{ username, password }` → Standbase forwards this (plus the client IP
   and User-Agent) to Aldrop
2. If the user has TOTP enabled, the response is `{ status: "totp_required", preAuthToken: "..." }` — this is Aldrop's TOTP challenge token, valid for one attempt and expiring in 5 minutes
3. `POST /auth/totp/verify` with `{ preAuthToken, totpCode }` → `{ status: "ok", sessionToken: "..." }`
4. If TOTP is not enabled, step 1 returns the session token directly
5. Every authenticated request calls Aldrop's `/auth/validate`, which enforces the session's 4-hour
   TTL and its IP + User-Agent device binding — Standbase does no local session storage or caching

## Rate Limiting

All limits are in-memory (per instance) and reset on restart.

| Filter | Applies to | Key | Limit |
|--------|------------|-----|-------|
| `AuthRateLimitFilter` | `POST /auth/login`, `POST /auth/totp/verify` | client IP | 10 failed (`401`) attempts per 15 minutes |
| `InvalidTokenRateLimitFilter` | any request with a Bearer token | client IP | 5 rejected tokens per hour, so bad tokens can't be used to hammer Aldrop |
| `EntryRateLimitFilter` | `/entry/**`, `/session/**`, `/heatmap/**` | user | 30 requests per minute |

Exceeded limits return `429` with a `Retry-After` header. `/todos` is not rate limited by Standbase.

## Project Structure

```
src/main/java/com/shivankkapoor/standbase/
├── config/         # SecurityConfig, AldropClientConfig
├── controller/     # AuthController, SessionController, EntryController, TodoController,
│                   # HeatMapController, MainController, GlobalExceptionHandler
├── dto/
│   ├── request/    # LoginRequestDTO, TotpVerifyRequestDTO, CreateEntryRequestDTO,
│   │               # CreateTodoRequestDTO, UpdateTodoRequestDTO, ReorderTodosRequestDTO
│   ├── response/   # LoginResponseDTO, CheckResponseDTO, EntryListResponseDTO,
│   │               # CreateEntryResponseDTO, EntryOverviewResponseDTO, HeatMapResponseDTO,
│   │               # TodoResponseDTO, TodoSummaryResponseDTO, ResponseDTO
│   └── aldrop/     # Request/response records for Aldrop's login, verify-totp,
│                   # validate, and logout endpoints
├── filter/         # SessionAuthFilter, AuthRateLimitFilter, InvalidTokenRateLimitFilter,
│                   # EntryRateLimitFilter
├── model/          # User, Entry, Todo, DayType, EntryLength
├── repository/     # UserRepository, EntryRepository, TodoRepository, HealthRepository
└── service/        # AuthService, EntryService, TodoService, HeatMapService,
                    # IpService, HealthService, DiscordService
```
