package com.friendsmp.singhamcore.service;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class DiscordSRVHook {

    private final boolean available;
    private final Object discordPlugin;
    private final Method getMainTextChannel;
    private final Method sendMessageMethod;
    private final Method queueMethod;

    public DiscordSRVHook(SinghamCorePlugin plugin) {
        Object instance = null;
        Method mainTextChannel = null;
        Method sendMessage = null;
        Method queue = null;
        boolean active = false;
        try {
            Plugin loaded = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
            if (loaded != null) {
                Class<?> discordClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
                Method getPluginMethod = discordClass.getMethod("getPlugin");
                instance = getPluginMethod.invoke(null);
                mainTextChannel = discordClass.getMethod("getMainTextChannel");
                Class<?> textChannel = Class.forName("net.dv8tion.jda.api.entities.TextChannel");
                sendMessage = textChannel.getMethod("sendMessage", String.class);
                queue = Class.forName("net.dv8tion.jda.api.requests.RestAction").getMethod("queue");
                active = instance != null && mainTextChannel != null && sendMessage != null && queue != null;
            }
        } catch (Throwable ignored) {
            active = false;
        }
        this.available = active;
        this.discordPlugin = instance;
        this.getMainTextChannel = mainTextChannel;
        this.sendMessageMethod = sendMessage;
        this.queueMethod = queue;
    }

    public boolean isAvailable() {
        return available;
    }

    public void sendMessage(String content) {
        if (!available || discordPlugin == null || getMainTextChannel == null || sendMessageMethod == null || queueMethod == null) {
            return;
        }
        try {
            Object channel = getMainTextChannel.invoke(discordPlugin);
            if (channel == null) {
                return;
            }
            Object action = sendMessageMethod.invoke(channel, content);
            if (action != null) {
                queueMethod.invoke(action);
            }
        } catch (Throwable ignored) {
            // fail gracefully
        }
    }
}
