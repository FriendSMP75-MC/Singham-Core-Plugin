package com.friendsmp.singhamcore.listeners;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.service.AuthService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class AuthListener implements Listener {

    private final AuthService authService;

    public AuthListener(SinghamCorePlugin plugin) {
        this.authService = plugin.getAuthService();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        authService.onDisconnect(uuid);
    }
}
