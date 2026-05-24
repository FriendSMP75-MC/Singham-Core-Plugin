-- MySQL/MariaDB migration for Singham Core
CREATE TABLE IF NOT EXISTS players (
    player_uuid CHAR(36) PRIMARY KEY,
    last_known_name TEXT,
    first_seen BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,
    last_ip VARCHAR(45)
);

CREATE TABLE IF NOT EXISTS punishments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36) NOT NULL,
    player_name TEXT,
    punishment_type VARCHAR(32) NOT NULL,
    moderator_uuid CHAR(36),
    moderator_name TEXT,
    reason TEXT,
    duration BIGINT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,
    ip_address VARCHAR(45),
    active BOOLEAN DEFAULT TRUE,
    revoked BOOLEAN DEFAULT FALSE,
    server_origin VARCHAR(255)
);

CREATE INDEX idx_punishments_player_uuid ON punishments(player_uuid);
CREATE INDEX idx_punishments_active ON punishments(active);
CREATE INDEX idx_punishments_expires_at ON punishments(expires_at);
CREATE INDEX idx_punishments_type ON punishments(punishment_type);

CREATE TABLE IF NOT EXISTS warnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36) NOT NULL,
    moderator_uuid CHAR(36),
    moderator_name TEXT,
    reason TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX idx_warnings_player_uuid ON warnings(player_uuid);

CREATE TABLE IF NOT EXISTS notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36) NOT NULL,
    author_uuid CHAR(36),
    author_name TEXT,
    note_text TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX idx_notes_player_uuid ON notes(player_uuid);

CREATE TABLE IF NOT EXISTS staff_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_uuid CHAR(36),
    action TEXT,
    target_uuid CHAR(36),
    target_name TEXT,
    reason TEXT,
    created_at BIGINT NOT NULL
);

CREATE INDEX idx_staff_logs_staff_uuid ON staff_logs(staff_uuid);
