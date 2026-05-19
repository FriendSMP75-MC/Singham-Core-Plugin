package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class ReputationRecord {

    private final UUID playerUuid;
    private final int score;
    private final Instant updatedAt;

    public ReputationRecord(UUID playerUuid, int score, Instant updatedAt) {
        this.playerUuid = playerUuid;
        this.score = score;
        this.updatedAt = updatedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getScore() {
        return score;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
