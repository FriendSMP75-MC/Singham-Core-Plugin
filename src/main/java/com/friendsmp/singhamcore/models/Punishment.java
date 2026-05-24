package com.friendsmp.singhamcore.models;

import com.friendsmp.singhamcore.punishments.PunishmentType;

import java.time.Instant;
import java.util.UUID;

public class Punishment {

    private long id;
    private final UUID playerUuid;
    private final String playerName;
    private final PunishmentType type;
    private final String moderator;
    private final String reason;
    private final long duration;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String ipAddress;
    private boolean active;

    public Punishment(long id, UUID playerUuid, String playerName, PunishmentType type, String moderator,
                      String reason, long duration, Instant createdAt, Instant expiresAt,
                      String ipAddress, boolean active) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.type = type;
        this.moderator = moderator;
        this.reason = reason;
        this.duration = duration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getModerator() {
        return moderator;
    }

    public String getReason() {
        return reason;
    }

    public long getDuration() {
        return duration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}