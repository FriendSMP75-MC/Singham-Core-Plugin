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
