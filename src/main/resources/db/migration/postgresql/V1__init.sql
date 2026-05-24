-- PostgreSQL migration for Singham Core
CREATE TABLE IF NOT EXISTS players (
    player_uuid UUID PRIMARY KEY,
    last_known_name TEXT,
    first_seen BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,
    last_ip TEXT
);

CREATE TABLE IF NOT EXISTS punishments (
    id BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL,
    player_name TEXT,
    punishment_type VARCHAR(32) NOT NULL,
    moderator_uuid UUID,
    moderator_name TEXT,
    reason TEXT,
    duration BIGINT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,
    ip_address TEXT,
    active BOOLEAN DEFAULT true,
    revoked BOOLEAN DEFAULT false,
    server_origin TEXT
);

CREATE INDEX IF NOT EXISTS idx_punishments_player_uuid ON punishments(player_uuid);
CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments(active);
CREATE INDEX IF NOT EXISTS idx_punishments_expires_at ON punishments(expires_at);
CREATE INDEX IF NOT EXISTS idx_punishments_type ON punishments(punishment_type);

CREATE TABLE IF NOT EXISTS warnings (
    id BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL,
    moderator_uuid UUID,
    moderator_name TEXT,
    reason TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_warnings_player_uuid ON warnings(player_uuid);

CREATE TABLE IF NOT EXISTS notes (
    id BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL,
    author_uuid UUID,
    author_name TEXT,
    note_text TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notes_player_uuid ON notes(player_uuid);

CREATE TABLE IF NOT EXISTS staff_logs (
    id BIGSERIAL PRIMARY KEY,
    staff_uuid UUID,
    action TEXT,
    target_uuid UUID,
    target_name TEXT,
    reason TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_staff_logs_staff_uuid ON staff_logs(staff_uuid);
