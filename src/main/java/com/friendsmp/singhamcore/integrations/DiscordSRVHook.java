package com.friendsmp.singhamcore.integrations;

import com.friendsmp.singhamcore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DiscordSRVHook {

    private final Plugin plugin;
    private final boolean available;

    public DiscordSRVHook(Plugin plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().isPluginEnabled("DiscordSRV");
    }

    public void sendVanishToggle(Player player, String messageTemplate) {
        if (!available || messageTemplate == null || messageTemplate.isBlank()) {
            return;
        }
        String message = TextUtils.stripColor(messageTemplate.replace("{player}", player.getName()));
        try {
            Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object discordSrv = discordSrvClass.getMethod("getPlugin").invoke(null);
            Object channel = discordSrv.getClass().getMethod("getMainTextChannel").invoke(discordSrv);
            if (channel == null) {
                return;
            }
            Object messageAction = channel.getClass().getMethod("sendMessage", CharSequence.class).invoke(channel, message);
            messageAction.getClass().getMethod("queue").invoke(messageAction);
        } catch (Exception exception) {
            plugin.getLogger().warning("DiscordSRV integration failed: " + exception.getMessage());
        }
    }
}
