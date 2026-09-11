# Standbase — Backend

The REST API for [Standbase](https://standbase.shivankkapoor.com), a personal standup journal. Built with Spring Boot 4 and Java 25, backed by PostgreSQL, and deployed via Cloudflare Tunnels.

> **Frontend repo:** [standbase-frontend](https://github.com/ShivankKapoor/standbase-frontend)

## Tech Stack

- **Java 25** / **Spring Boot 4.0**
- **PostgreSQL** — with `tsvector` full-text search on entry content
- **Spring Security** — stateless auth via a custom filter that delegates every login, TOTP
  verification, and session check to [Aldrop](https://github.com/ShivankKapoor/aldrop), a
  standalone auth microservice — see [Auth Flow](#auth-flow)
- **Bucket4j + Caffeine** — in-memory rate limiting on auth and entry endpoints
- **Lombok** — boilerplate reduction
- **Cloudflare Tunnels** — public exposure; real client IP via `CF-Connecting-IP`
- **Discord Webhooks** — login/logout/TOTP notifications

## Features

- Two-step login: password → TOTP (if enabled), each step issuing a short-lived token, both
  handled by Aldrop
- Sessions are device-bound (IP + User-Agent) by Aldrop — every request is validated against it,
  so a revoked or expired session stops working immediately
- Per-user rate limiting on login, TOTP verification, and entry writes
- Full-text search index on entry content via PostgreSQL `tsvector` trigger

## Prerequisites

- Java 25+
- PostgreSQL (run `schema.sql` to initialise tables, indexes, and the `standbase_app` role)
- A running [Aldrop](https://github.com/ShivankKapoor/aldrop) instance with a platform registered
  for Standbase (see that repo's README for platform setup)

## Setup

### 1. Database

Run `schema.sql` against your PostgreSQL instance as a superuser:

```bash
psql -U postgres -d standbase -f schema.sql
```

This creates the `users`, `entries`, and `todos` tables, all indexes, the `tsvector` trigger, and
the least-privilege `standbase_app` role.

### 2. Environment

Create a `.env` file in the project root (see `example.env` for all variables):

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

### 3. Create a User

There is no local registration endpoint — user accounts live entirely in Aldrop. Register the
user against Aldrop directly (`POST /auth/register` on the Standbase platform), using the same
id as any existing Standbase `users` row if you're migrating one. Standbase provisions its own
local `users` row automatically the first time that Aldrop user authenticates.

### 4. Run

```bash
./gradlew bootRun
```

## API

### Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Health check — returns app name, status, Java version |
| `POST` | `/auth/login` | Step 1 of login — returns session token or `totp_required` |
| `POST` | `/auth/totp/verify` | Step 2 of login — exchange pre-auth token + OTP for session token |

### Authenticated

All endpoints below require `Authorization: Bearer <token>`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/session/check` | Validate session — returns username |
| `POST` | `/session/logout` | Invalidate session token |
| `GET` | `/entry?year=&month=` | List all entries for a given month |
| `GET` | `/entry/{date}` | Get a single entry by date (`YYYY-MM-DD`) |
| `POST` | `/entry` | Create or update an entry |
| `DELETE` | `/entry/{date}` | Delete an entry by date |
| `GET` | `/heatmap` | Word-count history for the entry heatmap |
| `GET` | `/todos?date=` | List todos for a given date |
| `GET` | `/todos/summary?year=&month=` | Todo completion summary for a given month |
| `POST` | `/todos` | Create a todo |
| `PUT` | `/todos/{id}` | Update a todo |
| `PUT` | `/todos/reorder` | Reorder todos |
| `DELETE` | `/todos/{id}` | Delete a todo |

## Auth Flow

Standbase holds no passwords, TOTP secrets, or session state of its own — every step below is a
call through to Aldrop.

1. `POST /auth/login` with `{ username, password }` → Standbase forwards this to Aldrop
2. If the user has TOTP enabled, the response is `{ status: "totp_required", preAuthToken: "..." }` — this is Aldrop's TOTP challenge token, valid for one attempt and expiring in 5 minutes
3. `POST /auth/totp/verify` with `{ preAuthToken, totpCode }` → `{ status: "ok", sessionToken: "..." }`
4. If TOTP is not enabled, step 1 returns the session token directly
5. Every authenticated request calls Aldrop's `/auth/validate`, which enforces the session's 4-hour
   TTL and its IP + User-Agent device binding — Standbase does no local session storage or caching

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
├── filter/         # SessionAuthFilter, AuthRateLimitFilter,
│                   # EntryRateLimitFilter, AdminRateLimitFilter
├── model/          # User, Entry, Todo, DayType, EntryLength
├── repository/     # UserRepository, EntryRepository, TodoRepository, HealthRepository
└── service/        # AuthService, EntryService, TodoService, HeatMapService,
                    # IpService, HealthService, DiscordService
```
