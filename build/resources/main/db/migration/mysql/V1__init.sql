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

CREATE TABLE IF NOT EXISTS reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_uuid CHAR(36) NOT NULL,
    reported_uuid CHAR(36) NOT NULL,
    reported_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    claimed_by CHAR(36),
    claimed_at BIGINT,
    resolved_by CHAR(36),
    resolved_at BIGINT
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reported_uuid ON reports(reported_uuid);
CREATE INDEX idx_reports_claimed_by ON reports(claimed_by);

CREATE TABLE IF NOT EXISTS report_comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    staff_uuid CHAR(36) NOT NULL,
    note TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_report_comments_report FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE CASCADE
);

CREATE INDEX idx_report_comments_report_id ON report_comments(report_id);
CREATE INDEX idx_report_comments_staff_uuid ON report_comments(staff_uuid);

CREATE TABLE IF NOT EXISTS reputation (
    player_uuid CHAR(36) PRIMARY KEY,
    score INT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS reputation_transactions (
    tx_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_uuid CHAR(36) NOT NULL,
    player_uuid CHAR(36) NOT NULL,
    delta INT NOT NULL,
    reason TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    cooldown_key VARCHAR(255)
);

CREATE INDEX idx_reputation_transactions_player_uuid ON reputation_transactions(player_uuid);
CREATE INDEX idx_reputation_transactions_staff_uuid ON reputation_transactions(staff_uuid);
CREATE INDEX idx_reputation_transactions_created_at ON reputation_transactions(created_at);

CREATE TABLE IF NOT EXISTS staff_pins (
    staff_uuid CHAR(36) PRIMARY KEY,
    pin_hash TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS staff_sessions (
    session_id CHAR(36) PRIMARY KEY,
    staff_uuid CHAR(36) NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_staff_sessions_staff_uuid ON staff_sessions(staff_uuid);
CREATE INDEX idx_staff_sessions_active_expires ON staff_sessions(active, expires_at);
