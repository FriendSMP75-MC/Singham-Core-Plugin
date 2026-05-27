package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class ReputationTransaction {

    private final long txId;
    private final UUID staffUuid;
    private final UUID playerUuid;
    private final int delta;
    private final String reason;
    private final Instant createdAt;

    public ReputationTransaction(long txId, UUID staffUuid, UUID playerUuid, int delta, String reason, Instant createdAt) {
        this.txId = txId;
        this.staffUuid = staffUuid;
        this.playerUuid = playerUuid;
        this.delta = delta;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public long getTxId() {
        return txId;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getDelta() {
        return delta;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
