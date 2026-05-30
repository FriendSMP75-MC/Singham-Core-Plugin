-- SQLite migration for Singham Core
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS players (
    player_uuid TEXT PRIMARY KEY,
    last_known_name TEXT,
    first_seen INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    last_ip TEXT
);

CREATE TABLE IF NOT EXISTS punishments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    player_name TEXT,
    punishment_type TEXT NOT NULL,
    moderator_uuid TEXT,
    moderator_name TEXT,
    reason TEXT,
    duration INTEGER,
    created_at INTEGER NOT NULL,
    expires_at INTEGER,
    ip_address TEXT,
    active INTEGER DEFAULT 1,
    revoked INTEGER DEFAULT 0,
    server_origin TEXT
);

CREATE INDEX IF NOT EXISTS idx_punishments_player_uuid ON punishments(player_uuid);
CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments(active);
CREATE INDEX IF NOT EXISTS idx_punishments_expires_at ON punishments(expires_at);
CREATE INDEX IF NOT EXISTS idx_punishments_type ON punishments(punishment_type);

CREATE TABLE IF NOT EXISTS warnings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    moderator_uuid TEXT,
    moderator_name TEXT,
    reason TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_warnings_player_uuid ON warnings(player_uuid);

CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    author_uuid TEXT,
    author_name TEXT,
    note_text TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notes_player_uuid ON notes(player_uuid);

CREATE TABLE IF NOT EXISTS staff_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    staff_uuid TEXT,
    action TEXT,
    target_uuid TEXT,
    target_name TEXT,
    reason TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_staff_logs_staff_uuid ON staff_logs(staff_uuid);

CREATE TABLE IF NOT EXISTS reports (
    report_id INTEGER PRIMARY KEY AUTOINCREMENT,
    reporter_uuid TEXT NOT NULL,
    reported_uuid TEXT NOT NULL,
    reported_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL,
    claimed_by TEXT,
    claimed_at INTEGER,
    resolved_by TEXT,
    resolved_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_reported_uuid ON reports(reported_uuid);
CREATE INDEX IF NOT EXISTS idx_reports_claimed_by ON reports(claimed_by);

CREATE TABLE IF NOT EXISTS report_comments (
    comment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_id INTEGER NOT NULL,
    staff_uuid TEXT NOT NULL,
    note TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_report_comments_report_id ON report_comments(report_id);
CREATE INDEX IF NOT EXISTS idx_report_comments_staff_uuid ON report_comments(staff_uuid);

CREATE TABLE IF NOT EXISTS reputation (
    player_uuid TEXT PRIMARY KEY,
    score INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS reputation_transactions (
    tx_id INTEGER PRIMARY KEY AUTOINCREMENT,
    staff_uuid TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    cooldown_key TEXT
);

CREATE INDEX IF NOT EXISTS idx_reputation_transactions_player_uuid ON reputation_transactions(player_uuid);
CREATE INDEX IF NOT EXISTS idx_reputation_transactions_staff_uuid ON reputation_transactions(staff_uuid);
CREATE INDEX IF NOT EXISTS idx_reputation_transactions_created_at ON reputation_transactions(created_at);

CREATE TABLE IF NOT EXISTS staff_pins (
    staff_uuid TEXT PRIMARY KEY,
    pin_hash TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS staff_sessions (
    session_id TEXT PRIMARY KEY,
    staff_uuid TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    last_seen_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_staff_sessions_staff_uuid ON staff_sessions(staff_uuid);
CREATE INDEX IF NOT EXISTS idx_staff_sessions_active_expires ON staff_sessions(active, expires_at);
