package com.friendsmp.singhamcore.repository;

import com.friendsmp.singhamcore.models.Punishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentRepository {
    CompletableFuture<Void> save(Punishment punishment);
    CompletableFuture<List<Punishment>> loadActivePunishments();
    CompletableFuture<Optional<Punishment>> findActiveByPlayerUuid(UUID playerUuid);
    CompletableFuture<Void> updateActive(long id, boolean active);
    CompletableFuture<List<Punishment>> loadHistory(UUID playerUuid);
}
