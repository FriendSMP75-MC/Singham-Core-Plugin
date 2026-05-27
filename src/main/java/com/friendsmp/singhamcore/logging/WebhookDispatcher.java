package com.friendsmp.singhamcore.logging;

import com.friendsmp.singhamcore.SinghamCorePlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class WebhookDispatcher {

    private final SinghamCorePlugin plugin;
    private final String webhookUrl;
    private final boolean enabled;

    public WebhookDispatcher(SinghamCorePlugin plugin) {
        this.plugin = plugin;
        this.webhookUrl = plugin.getConfig().getString("logging.webhook-url", "").trim();
        this.enabled = !this.webhookUrl.isEmpty() && plugin.getConfig().getBoolean("logging.webhook-enabled", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void dispatch(String content) {
        if (!enabled || webhookUrl.isBlank()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                String payload = "{\"content\":\"" + escapeJson(content) + "\"}";
                byte[] body = payload.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body);
                }
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    plugin.getLogger().warning("Webhook dispatch returned status " + status);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to dispatch webhook: " + e.getMessage());
            }
        });
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
