# Standbase

A personal standup/journal REST API built with Spring Boot 4.0 and Java 25. Hosted via Cloudflare tunnels with a PostgreSQL backend.

## Tech Stack

- **Java 25** / **Spring Boot 4.0**
- **PostgreSQL 18** — with full-text search via `tsvector`
- **Spring Security** — stateless session auth via custom filter
- **Cloudflare Tunnels** — public exposure, real IP via `CF-Connecting-IP`

## Prerequisites

- Java 25+
- PostgreSQL (run `schema.sql` to set up tables and the `standbase_app` user)
- A `.env` file in the project root (see below)

## Setup

### 1. Database

Run `schema.sql` against your PostgreSQL instance as a superuser:

```bash
psql -U postgres -d standbase_qa -f schema.sql
```

### 2. Environment

Create a `.env` file in the project root:

```env
DB_URL=jdbc:postgresql://<host>:<port>/<dbname>
DB_USER=standbase_app
DB_PASSWORD=<your_password>
```

### 3. Create a User

The API has no public registration endpoint. Use the included Python script to create a user directly in the database.

**Requires [uv](https://docs.astral.sh/uv/) — install once:**

```bash
# macOS/Linux
curl -LsSf https://astral.sh/uv/install.sh | sh

# Windows (PowerShell)
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

**Then run:**

```bash
uv run create_user.py
```

`uv` handles the virtual environment and dependencies automatically. The script reads your `.env`, prompts for a username and password, BCrypt-hashes the password, and inserts the user.

### 4. Run

```bash
./gradlew bootRun
```

## API

### Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Health check — returns name, status, Java version |
| POST | `/auth/login` | Login — returns session token |

### Authenticated

All other endpoints require `Authorization: Bearer <token>`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/logout` | Logout — invalidates session token |

## Auth Flow

1. POST credentials to `/auth/login`
2. Receive a session token
3. Send `Authorization: Bearer <token>` on all subsequent requests
4. Sessions expire after 30 minutes
5. Token is bound to the originating IP — requests from a different IP will be rejected

## Project Structure

```
src/main/java/com/shivankkapoor/standbase/
├── config/         # SecurityConfig
├── controller/     # MainController, AuthController, GlobalExceptionHandler
├── dto/            # LoginRequestDTO, LoginResponseDTO, ResponseDTO
├── filter/         # SessionAuthFilter
├── model/          # User, Entry (JPA records)
├── repository/     # UserRepository, EntryRepository
└── service/        # AuthService, SessionService, IpService
```
