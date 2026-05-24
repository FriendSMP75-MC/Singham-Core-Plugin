package com.friendsmp.singhamcore.repository;

import com.friendsmp.singhamcore.models.Warning;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface WarningRepository {
    CompletableFuture<Void> addWarning(Warning warning);
    CompletableFuture<List<Warning>> listWarnings(UUID playerUuid);
}
