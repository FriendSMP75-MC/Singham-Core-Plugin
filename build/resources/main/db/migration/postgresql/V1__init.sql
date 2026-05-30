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

CREATE TABLE IF NOT EXISTS reports (
    report_id BIGSERIAL PRIMARY KEY,
    reporter_uuid UUID NOT NULL,
    reported_uuid UUID NOT NULL,
    reported_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    status TEXT NOT NULL,
    claimed_by UUID,
    claimed_at BIGINT,
    resolved_by UUID,
    resolved_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_reported_uuid ON reports(reported_uuid);
CREATE INDEX IF NOT EXISTS idx_reports_claimed_by ON reports(claimed_by);

CREATE TABLE IF NOT EXISTS report_comments (
    comment_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(report_id) ON DELETE CASCADE,
    staff_uuid UUID NOT NULL,
    note TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_report_comments_report_id ON report_comments(report_id);
CREATE INDEX IF NOT EXISTS idx_report_comments_staff_uuid ON report_comments(staff_uuid);

CREATE TABLE IF NOT EXISTS reputation (
    player_uuid UUID PRIMARY KEY,
    score INTEGER NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS reputation_transactions (
    tx_id BIGSERIAL PRIMARY KEY,
    staff_uuid UUID NOT NULL,
    player_uuid UUID NOT NULL,
    delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    cooldown_key TEXT
);

CREATE INDEX IF NOT EXISTS idx_reputation_transactions_player_uuid ON reputation_transactions(player_uuid);
CREATE INDEX IF NOT EXISTS idx_reputation_transactions_staff_uuid ON reputation_transactions(staff_uuid);
CREATE INDEX IF NOT EXISTS idx_reputation_transactions_created_at ON reputation_transactions(created_at);

CREATE TABLE IF NOT EXISTS staff_pins (
    staff_uuid UUID PRIMARY KEY,
    pin_hash TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS staff_sessions (
    session_id UUID PRIMARY KEY,
    staff_uuid UUID NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_staff_sessions_staff_uuid ON staff_sessions(staff_uuid);
CREATE INDEX IF NOT EXISTS idx_staff_sessions_active_expires ON staff_sessions(active, expires_at);
