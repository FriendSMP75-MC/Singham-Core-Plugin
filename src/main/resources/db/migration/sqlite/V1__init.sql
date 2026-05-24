-- Singham Core initial schema (SQLite)

CREATE TABLE IF NOT EXISTS players (
    uuid TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    updated_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS punishments (
    punishment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT,
    player_name TEXT,
    punishment_type TEXT NOT NULL,
    moderator TEXT NOT NULL,
    reason TEXT NOT NULL,
    duration_ms INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    expires_at TIMESTAMP,
    ip_address TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    revoked_at TIMESTAMP,
    revoked_by TEXT
);

CREATE INDEX IF NOT EXISTS punishments_active_idx ON punishments (active);
CREATE INDEX IF NOT EXISTS punishments_player_uuid_idx ON punishments (player_uuid);
CREATE INDEX IF NOT EXISTS punishments_expires_at_idx ON punishments (expires_at);
CREATE INDEX IF NOT EXISTS punishments_ip_address_idx ON punishments (ip_address);
CREATE INDEX IF NOT EXISTS punishments_type_idx ON punishments (punishment_type);

CREATE TABLE IF NOT EXISTS staff_pins (
    staff_uuid TEXT PRIMARY KEY,
    pin_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    updated_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS staff_sessions (
    session_id TEXT PRIMARY KEY,
    staff_uuid TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    last_seen_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    expires_at TIMESTAMP NOT NULL,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS staff_sessions_staff_uuid_idx ON staff_sessions (staff_uuid);
CREATE INDEX IF NOT EXISTS staff_sessions_active_expires_idx ON staff_sessions (active, expires_at);

CREATE TABLE IF NOT EXISTS reputation (
    player_uuid TEXT PRIMARY KEY,
    score INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS reputation_transactions (
    tx_id INTEGER PRIMARY KEY AUTOINCREMENT,
    staff_uuid TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    cooldown_key TEXT
);

CREATE INDEX IF NOT EXISTS rep_tx_player_idx ON reputation_transactions (player_uuid);
CREATE INDEX IF NOT EXISTS rep_tx_staff_idx ON reputation_transactions (staff_uuid);
CREATE INDEX IF NOT EXISTS rep_tx_created_at_idx ON reputation_transactions (created_at);

CREATE TABLE IF NOT EXISTS reports (
    report_id INTEGER PRIMARY KEY AUTOINCREMENT,
    reporter_uuid TEXT NOT NULL,
    reported_uuid TEXT NOT NULL,
    reported_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now')),
    status TEXT NOT NULL,
    claimed_by TEXT,
    claimed_at TIMESTAMP,
    resolved_by TEXT,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS reports_status_idx ON reports (status);
CREATE INDEX IF NOT EXISTS reports_reported_uuid_idx ON reports (reported_uuid);
CREATE INDEX IF NOT EXISTS reports_claimed_by_idx ON reports (claimed_by);

CREATE TABLE IF NOT EXISTS report_comments (
    comment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL REFERENCES reports(report_id) ON DELETE CASCADE,
    staff_uuid TEXT NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS report_comments_report_idx ON report_comments (report_id);
CREATE INDEX IF NOT EXISTS report_comments_staff_idx ON report_comments (staff_uuid);

CREATE TABLE IF NOT EXISTS staff_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    staff_uuid TEXT NOT NULL,
    action TEXT NOT NULL,
    target_uuid TEXT,
    target_name TEXT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS staff_logs_staff_uuid_idx ON staff_logs (staff_uuid);
CREATE INDEX IF NOT EXISTS staff_logs_created_at_idx ON staff_logs (created_at);

CREATE TABLE IF NOT EXISTS staff_notes (
    note_id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    staff_uuid TEXT NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS staff_notes_player_uuid_idx ON staff_notes (player_uuid);
