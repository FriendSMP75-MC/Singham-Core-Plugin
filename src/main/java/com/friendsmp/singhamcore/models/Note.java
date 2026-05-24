package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class Note {
    private long id;
    private final UUID playerUuid;
    private final UUID authorUuid;
    private final String authorName;
    private final String text;
    private final Instant createdAt;

    public Note(long id, UUID playerUuid, UUID authorUuid, String authorName, String text, Instant createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public UUID getAuthorUuid() { return authorUuid; }
    public String getAuthorName() { return authorName; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
