-- Singham Core initial schema (v1)
-- Use timestamps in UTC

CREATE TABLE IF NOT EXISTS players (
    uuid UUID PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE IF NOT EXISTS punishments (
    punishment_id BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL,
    player_name TEXT NOT NULL,
    punishment_type TEXT NOT NULL,
    moderator TEXT NOT NULL,
    reason TEXT NOT NULL,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    expires_at TIMESTAMP,
    ip_address TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMP,
    revoked_by TEXT
);

CREATE INDEX IF NOT EXISTS punishments_active_idx ON punishments (active);
CREATE INDEX IF NOT EXISTS punishments_player_uuid_idx ON punishments (player_uuid);
CREATE INDEX IF NOT EXISTS punishments_expires_at_idx ON punishments (expires_at);
CREATE INDEX IF NOT EXISTS punishments_ip_address_idx ON punishments (ip_address);
CREATE INDEX IF NOT EXISTS punishments_type_idx ON punishments (punishment_type);

CREATE TABLE IF NOT EXISTS staff_pins (
    staff_uuid UUID PRIMARY KEY,
    pin_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE IF NOT EXISTS staff_sessions (
    session_id UUID PRIMARY KEY,
    staff_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    last_seen_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS staff_sessions_staff_uuid_idx ON staff_sessions (staff_uuid);
CREATE INDEX IF NOT EXISTS staff_sessions_active_expires_idx ON staff_sessions (active, expires_at);

CREATE TABLE IF NOT EXISTS reputation (
    player_uuid UUID PRIMARY KEY,
    score INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE IF NOT EXISTS reputation_transactions (
    tx_id BIGSERIAL PRIMARY KEY,
    staff_uuid UUID NOT NULL,
    player_uuid UUID NOT NULL,
    delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    cooldown_key TEXT
);

CREATE INDEX IF NOT EXISTS rep_tx_player_idx ON reputation_transactions (player_uuid);
CREATE INDEX IF NOT EXISTS rep_tx_staff_idx ON reputation_transactions (staff_uuid);
CREATE INDEX IF NOT EXISTS rep_tx_created_at_idx ON reputation_transactions (created_at);

CREATE TABLE IF NOT EXISTS reports (
    report_id BIGSERIAL PRIMARY KEY,
    reporter_uuid UUID NOT NULL,
    reported_uuid UUID NOT NULL,
    reported_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    status TEXT NOT NULL,
    claimed_by UUID,
    claimed_at TIMESTAMP,
    resolved_by UUID,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS reports_status_idx ON reports (status);
CREATE INDEX IF NOT EXISTS reports_reported_uuid_idx ON reports (reported_uuid);
CREATE INDEX IF NOT EXISTS reports_claimed_by_idx ON reports (claimed_by);

CREATE TABLE IF NOT EXISTS report_comments (
    comment_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(report_id) ON DELETE CASCADE,
    staff_uuid UUID NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS report_comments_report_idx ON report_comments (report_id);
CREATE INDEX IF NOT EXISTS report_comments_staff_idx ON report_comments (staff_uuid);

CREATE TABLE IF NOT EXISTS staff_logs (
    log_id BIGSERIAL PRIMARY KEY,
    staff_uuid UUID NOT NULL,
    action TEXT NOT NULL,
    target_uuid UUID,
    target_name TEXT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS staff_logs_staff_uuid_idx ON staff_logs (staff_uuid);
CREATE INDEX IF NOT EXISTS staff_logs_created_at_idx ON staff_logs (created_at);

