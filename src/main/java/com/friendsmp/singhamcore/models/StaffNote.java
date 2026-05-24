package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class StaffNote {

    private final long id;
    private final UUID playerUuid;
    private final UUID staffUuid;
    private final String note;
    private final Instant createdAt;

    public StaffNote(long id, UUID playerUuid, UUID staffUuid, String note, Instant createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.staffUuid = staffUuid;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
