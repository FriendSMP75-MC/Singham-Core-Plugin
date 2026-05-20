# Singham Core - Implementation TODO

## Phase 0: Foundation & scaffolding upgrades
- [ ] Replace plugin.yml permissions/command definitions to match required `singham.*` and new command suite (`staffpin`, `unban`, `unmute`, `clearhistory`, `rep ...`, `report ...`, etc.)
- [ ] Introduce configuration loading: `config.yml`, `messages.yml`, `punishments.yml` via a MessageManager/ConfigManager
- [ ] Add an async database execution layer with a dedicated bounded ExecutorService for DB tasks

## Phase 1: PostgreSQL migrations + repository layer
- [ ] Add migration system (Flyway or custom versioned migrations)
- [ ] Replace DatabaseManager CREATE TABLE bootstrap with migrations
- [ ] Implement repository layer (PunishmentRepository, StaffPinRepository, ReputationRepository, ReportsRepository, StaffLogRepository, StaffSessionRepository)
- [ ] Add required schema + indexes:
  - [ ] players
  - [ ] punishments (with punishment_id, active, expires_at, ip_address, revoked metadata)
  - [ ] staff_pins
  - [ ] staff_sessions
  - [ ] reputation + reputation_transactions (cooldowns)
  - [ ] reports + report_comments
  - [ ] staff_logs

## Phase 2: Security - Staff PIN auth + protected command gating
- [ ] Implement BCrypt staff PIN hashing + verification
- [ ] Implement in-memory authenticated sessions cache (timeout + disconnect logout)
- [ ] Implement failed-login rate limiting + lockout
- [ ] Implement `/staffpin setup|login|logout|reset` commands
- [ ] Implement command gate: if sender has `singham.staff` but not authenticated -> block protected commands with required messages

## Phase 3: Moderation subsystem (fully functional)
- [ ] Implement DurationParser (1d/7h/30m etc)
- [ ] Implement PunishmentFormatter + Punishment templates/messages
- [ ] Implement ActivePunishmentCache (by player_uuid and ip_address for IP bans)
- [ ] Punishment revocation APIs: unban/unmute/revoke/clear history
- [ ] Automatic punishment expiry scheduler (revokes DB + cache)
- [ ] Join enforcement:
  - [ ] deny ban/tempban
  - [ ] deny IP bans (validate/normalize IP)
  - [ ] enforce active mutes via chat listener (AsyncPlayerChatEvent)
- [ ] Implement missing commands: `/unban`, `/unmute`, `/clearhistory`, `/history` pagination, `/check`, `/ipban`

## Phase 4: Reputation subsystem
- [ ] Implement reputation transactions, reasons, history, self-prevention
- [ ] Implement cooldown system per staff/player/command key
- [ ] Implement `/rep give|remove|history|top`
- [ ] Add leaderboard query and/or cached top list

## Phase 5: Reports subsystem
- [ ] Implement report queue and status transitions (OPEN -> IN_PROGRESS -> RESOLVED)
- [ ] Implement claim/resolve commands with authorization + staff auth gate
- [ ] Implement report comments/notes
- [ ] Implement notifications for online staff with proper permission
- [ ] Add `/reports` listing + history view

## Phase 6: Staff management subsystem
- [ ] Implement staff list/stats/notes/stafflog
- [ ] Implement moderation statistics + staff action history
- [ ] Audit every auth + moderation action to staff_logs (and/or dedicated audit table)

## Phase 7: Production readiness
- [ ] Thread-safety audit for caches
- [ ] Remove any fake data / placeholder behavior
- [ ] Add integration tests for duration parsing, parsing/validation, and SQL queries
- [ ] Run `./gradlew test` and `./gradlew build`

