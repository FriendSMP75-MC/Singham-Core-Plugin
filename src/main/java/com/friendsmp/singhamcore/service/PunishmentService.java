package com.friendsmp.singhamcore.service;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.models.Punishment;
import com.friendsmp.singhamcore.repository.PunishmentRepository;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentService {

    private final SinghamCorePlugin plugin;
    private final PunishmentRepository repository;
    private final Map<UUID, Punishment> activeCache = new ConcurrentHashMap<>();

    public PunishmentService(SinghamCorePlugin plugin, PunishmentRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<Void> loadActivePunishments() {
        return repository.loadActivePunishments().thenAccept(list -> {
            for (Punishment p : list) activeCache.put(p.getPlayerUuid(), p);
            scheduleExpirationTask();
        });
    }

    private void scheduleExpirationTask() {
        int refresh = plugin.getConfig().getInt("storage.cache-refresh-seconds", 30);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Instant now = Instant.now();
            activeCache.values().stream()
                    .filter(p -> p.getExpiresAt() != null && p.getExpiresAt().isBefore(now))
                    .toList()
                    .forEach(this::expirePunishment);
        }, 20L, refresh * 20L);
    }

    public CompletableFuture<Void> createPunishment(Punishment punishment) {
        if (punishment.isActive()) activeCache.put(punishment.getPlayerUuid(), punishment);
        return repository.save(punishment);
    }

    public void expirePunishment(Punishment punishment) {
        punishment.setActive(false);
        activeCache.remove(punishment.getPlayerUuid());
        repository.updateActive(punishment.getId(), false);
    }

    public boolean isPlayerPunished(UUID uuid) {
        return activeCache.containsKey(uuid);
    }

    public Punishment getActivePunishment(UUID uuid) {
        return activeCache.get(uuid);
    }

    public Collection<Punishment> getActivePunishments() {
        return activeCache.values();
    }
}
