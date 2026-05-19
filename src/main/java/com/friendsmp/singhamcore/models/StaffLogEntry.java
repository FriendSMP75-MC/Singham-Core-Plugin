package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class StaffLogEntry {

    private final UUID staffUuid;
    private final String action;
    private final UUID targetUuid;
    private final String targetName;
    private final String reason;
    private final Instant createdAt;

    public StaffLogEntry(UUID staffUuid, String action, UUID targetUuid, String targetName, String reason, Instant createdAt) {
        this.staffUuid = staffUuid;
        this.action = action;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public String getAction() {
        return action;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
