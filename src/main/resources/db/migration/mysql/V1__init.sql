-- Singham Core initial schema (MySQL/MariaDB)

CREATE TABLE IF NOT EXISTS players (
    uuid CHAR(36) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS punishments (
    punishment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36),
    player_name VARCHAR(64),
    punishment_type VARCHAR(32) NOT NULL,
    moderator VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    ip_address VARCHAR(45),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at TIMESTAMP NULL,
    revoked_by VARCHAR(64)
);

CREATE INDEX punishments_active_idx ON punishments (active);
CREATE INDEX punishments_player_uuid_idx ON punishments (player_uuid);
CREATE INDEX punishments_expires_at_idx ON punishments (expires_at);
CREATE INDEX punishments_ip_address_idx ON punishments (ip_address);
CREATE INDEX punishments_type_idx ON punishments (punishment_type);

CREATE TABLE IF NOT EXISTS staff_pins (
    staff_uuid CHAR(36) PRIMARY KEY,
    pin_hash TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staff_sessions (
    session_id CHAR(36) PRIMARY KEY,
    staff_uuid CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX staff_sessions_staff_uuid_idx ON staff_sessions (staff_uuid);
CREATE INDEX staff_sessions_active_expires_idx ON staff_sessions (active, expires_at);

CREATE TABLE IF NOT EXISTS reputation (
    player_uuid CHAR(36) PRIMARY KEY,
    score INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reputation_transactions (
    tx_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_uuid CHAR(36) NOT NULL,
    player_uuid CHAR(36) NOT NULL,
    delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cooldown_key VARCHAR(255)
);

CREATE INDEX rep_tx_player_idx ON reputation_transactions (player_uuid);
CREATE INDEX rep_tx_staff_idx ON reputation_transactions (staff_uuid);
CREATE INDEX rep_tx_created_at_idx ON reputation_transactions (created_at);

CREATE TABLE IF NOT EXISTS reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_uuid CHAR(36) NOT NULL,
    reported_uuid CHAR(36) NOT NULL,
    reported_name VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    claimed_by CHAR(36),
    claimed_at TIMESTAMP NULL,
    resolved_by CHAR(36),
    resolved_at TIMESTAMP NULL
);

CREATE INDEX reports_status_idx ON reports (status);
CREATE INDEX reports_reported_uuid_idx ON reports (reported_uuid);
CREATE INDEX reports_claimed_by_idx ON reports (claimed_by);

CREATE TABLE IF NOT EXISTS report_comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    staff_uuid CHAR(36) NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT report_comments_report_fk FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE CASCADE
);

CREATE INDEX report_comments_report_idx ON report_comments (report_id);
CREATE INDEX report_comments_staff_idx ON report_comments (staff_uuid);

CREATE TABLE IF NOT EXISTS staff_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_uuid CHAR(36) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_uuid CHAR(36),
    target_name VARCHAR(64),
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX staff_logs_staff_uuid_idx ON staff_logs (staff_uuid);
CREATE INDEX staff_logs_created_at_idx ON staff_logs (created_at);

CREATE TABLE IF NOT EXISTS staff_notes (
    note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36) NOT NULL,
    staff_uuid CHAR(36) NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX staff_notes_player_uuid_idx ON staff_notes (player_uuid);
