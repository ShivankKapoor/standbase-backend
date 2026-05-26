-- Setup Extensions & Schema Objects (Admin / Postgres User)
-- Note: Run this script after creating the standbase_QA database
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username     TEXT NOT NULL UNIQUE,
    password     TEXT NOT NULL,
    totp_secret  TEXT,
    totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS entries (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    entry_date   DATE NOT NULL,
    day_type     TEXT CHECK (day_type IN ('PTO', 'PLANNING', 'SUPPORT')),
    content      TEXT,
    search_vector TSVECTOR,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, entry_date)
);

CREATE INDEX IF NOT EXISTS entries_user_date_idx ON entries (user_id, entry_date DESC);
CREATE INDEX IF NOT EXISTS entries_search_vector_idx ON entries USING GIN (search_vector);

CREATE OR REPLACE FUNCTION entries_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS entries_search_vector_trigger ON entries;
CREATE TRIGGER entries_search_vector_trigger
    BEFORE INSERT OR UPDATE ON entries
    FOR EACH ROW EXECUTE FUNCTION entries_search_vector_update();

-- 2. Create the App User Role First
CREATE USER standbase_app WITH PASSWORD 'your_secure_password_here';

-- 3. Define Future Default Permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO standbase_app;

-- 4. Assign Database and Schema Level Entry
GRANT CONNECT ON DATABASE standbase_QA TO standbase_app;
GRANT USAGE ON SCHEMA public TO standbase_app;

-- 5. Hard-Lock Security (Explicitly prevent app user from changing schema structures)
REVOKE CREATE ON SCHEMA public FROM standbase_app;

-- 6. Apply Catch-All Rights to Currently Existing Objects
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO standbase_app;