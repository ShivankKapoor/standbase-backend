# Standbase — Backend

The REST API for [Standbase](https://standbase.shivankkapoor.com), a personal standup journal. Built with Spring Boot 4 and Java 25, backed by PostgreSQL, and deployed via Cloudflare Tunnels.

> **Frontend repo:** [standbase-frontend](https://github.com/ShivankKapoor/standbase-frontend)

## Tech Stack

- **Java 25** / **Spring Boot 4.0**
- **PostgreSQL** — with `tsvector` full-text search on entry content
- **Spring Security** — stateless session auth via a custom filter; sessions are IP-bound
- **TOTP (2FA)** — via `dev.samstevens.totp`; optional per user
- **Bucket4j + Caffeine** — in-memory rate limiting on auth and entry endpoints
- **Lombok** — boilerplate reduction
- **Cloudflare Tunnels** — public exposure; real client IP via `CF-Connecting-IP`
- **Discord Webhooks** — auth event notifications

## Features

- Two-step login: password → TOTP (if enabled), each step issuing a short-lived token
- Session tokens are IP-bound — requests from a different IP are rejected
- Per-user rate limiting on login, TOTP verification, and entry writes
- Auth event logging (login, logout, failures) with country/city via an IP geolocation service
- Full-text search index on entry content via PostgreSQL `tsvector` trigger

## Prerequisites

- Java 25+
- PostgreSQL (run `schema.sql` to initialise tables, indexes, and the `standbase_app` role)

## Setup

### 1. Database

Run `schema.sql` against your PostgreSQL instance as a superuser:

```bash
psql -U postgres -d standbase -f schema.sql
```

This creates the `users`, `entries`, `auth_events`, and `sessions` tables, all indexes, the `tsvector` trigger, and the least-privilege `standbase_app` role.

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
MERIDIAN_BASE_URL=        # IP geolocation service base URL
```

### 3. Create a User

There is no public registration endpoint. Use the included script to create a user directly:

**Requires [uv](https://docs.astral.sh/uv/):**

```bash
# macOS / Linux
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows (PowerShell)
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

```bash
uv run create_user.py
```

`uv` manages the virtual environment automatically. The script reads your `.env`, prompts for a username and password, BCrypt-hashes the password, and inserts the user.

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

## Auth Flow

1. `POST /auth/login` with `{ username, password }`
2. If the user has TOTP enabled, the response is `{ status: "totp_required", preAuthToken: "..." }` — the pre-auth token is valid for one TOTP attempt and expires quickly
3. `POST /auth/totp/verify` with `{ preAuthToken, totpCode }` → `{ status: "ok", sessionToken: "..." }`
4. If TOTP is not enabled, step 1 returns the session token directly
5. Session tokens expire after 4 hours and are bound to the originating IP

## Project Structure

```
src/main/java/com/shivankkapoor/standbase/
├── config/         # SecurityConfig
├── controller/     # AuthController, SessionController, EntryController,
│                   # MainController, GlobalExceptionHandler
├── dto/
│   ├── request/    # LoginRequestDTO, TotpVerifyRequestDTO, CreateEntryRequestDTO
│   └── response/   # LoginResponseDTO, CheckResponseDTO, EntryListResponseDTO,
│                   # CreateEntryResponseDTO, EntryOverviewResponseDTO, ResponseDTO
├── filter/         # SessionAuthFilter, AuthRateLimitFilter,
│                   # EntryRateLimitFilter, AdminRateLimitFilter
├── model/          # User, Entry, Session, AuthEvent, DayType, AuthEventType
├── repository/     # UserRepository, EntryRepository, SessionRepository,
│                   # AuthEventRepository, HealthRepository
└── service/        # AuthService, SessionService, PreAuthService, EntryService,
                    # AuthEventService, IpService, HealthService, DiscordService
```
