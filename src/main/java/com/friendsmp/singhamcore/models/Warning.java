package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class Warning {
    private long id;
    private final UUID playerUuid;
    private final UUID moderatorUuid;
    private final String moderatorName;
    private final String reason;
    private final Instant createdAt;

    public Warning(long id, UUID playerUuid, UUID moderatorUuid, String moderatorName, String reason, Instant createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.moderatorUuid = moderatorUuid;
        this.moderatorName = moderatorName;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public UUID getModeratorUuid() { return moderatorUuid; }
    public String getModeratorName() { return moderatorName; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
