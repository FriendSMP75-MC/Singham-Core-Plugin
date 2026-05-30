package com.friendsmp.singhamcore;

import com.friendsmp.singhamcore.commands.CommandManager;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.managers.PunishmentManager;
import com.friendsmp.singhamcore.managers.ReputationManager;
import com.friendsmp.singhamcore.managers.ReportManager;
import com.friendsmp.singhamcore.managers.StaffLogManager;
import com.friendsmp.singhamcore.managers.ChatLockManager;
import com.friendsmp.singhamcore.managers.VanishManager;
import com.friendsmp.singhamcore.utils.ConfigUtils;
import com.friendsmp.singhamcore.utils.TextUtils;
import com.friendsmp.singhamcore.listeners.VanishListener;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SinghamCorePlugin extends JavaPlugin {

    private static SinghamCorePlugin instance;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private ReputationManager reputationManager;
    private ReportManager reportManager;
    private StaffLogManager staffLogManager;
    private VanishManager vanishManager;
    private com.friendsmp.singhamcore.service.AuthService authService;
    private com.friendsmp.singhamcore.logging.AuditLogger auditLogger;
    private CommandManager commandManager;
    private ChatLockManager chatLockManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ConfigUtils.loadConfig(getConfig());

        databaseManager = new DatabaseManager(this);
        authService = new com.friendsmp.singhamcore.service.AuthService(this);
        auditLogger = new com.friendsmp.singhamcore.logging.AuditLogger(this);
        punishmentManager = new PunishmentManager(this, databaseManager);
        reputationManager = new ReputationManager(this, databaseManager);
        reportManager = new ReportManager(this, databaseManager);
        staffLogManager = new StaffLogManager(this, databaseManager);
        vanishManager = new VanishManager(this);
        commandManager = new CommandManager(this);

        // register listeners
        registerListeners();
        commandManager.registerCommands();
        logStartupHealth();
        getLogger().info("Singham Core enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("Singham Core disabled.");
    }

    public static SinghamCorePlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public ReputationManager getReputationManager() {
        return reputationManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public StaffLogManager getStaffLogManager() {
        return staffLogManager;
    }

    public com.friendsmp.singhamcore.service.AuthService getAuthService() {
        return authService;
    }

    public com.friendsmp.singhamcore.logging.AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public ChatLockManager getChatLockManager() {
        return chatLockManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public boolean ensureStaffAuth(CommandSender sender) {
        if (sender instanceof Player player) {
            boolean expired = authService.sessionExpired(player.getUniqueId());
            if (!authService.isAuthenticated(player.getUniqueId())) {
                if (expired) {
                    player.sendMessage(TextUtils.color(getConfig().getString("messages.prefix") + getConfig().getString("messages.staffauth-session-expired")));
                } else {
                    player.sendMessage(TextUtils.color(getConfig().getString("messages.prefix") + getConfig().getString("messages.staffauth-required")));
                }
                auditLogger.log("auth.failure", "Staff authentication required", java.util.Map.of(
                        "sender", player.getName(),
                        "uuid", player.getUniqueId().toString()));
                return false;
            }
        }
        return true;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.PlayerConnectionListener(punishmentManager), this);
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.AuthListener(this), this);
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.SensitiveCommandListener(this), this);
        // Chat control (mutes & global lock)
        chatLockManager = new com.friendsmp.singhamcore.managers.ChatLockManager();
        getServer().getPluginManager().registerEvents(new com.friendsmp.singhamcore.listeners.ChatControlListener(this, chatLockManager), this);
        getServer().getPluginManager().registerEvents(new VanishListener(vanishManager), this);
    }

    private void logStartupHealth() {
        if (databaseManager == null || !databaseManager.isAvailable()) {
            getLogger().warning("[SinghamCore] Running in DEGRADED mode");
            return;
        }

        getLogger().info("[SinghamCore] Database connected");
        if (databaseManager.migrationsApplied()) {
            getLogger().info("[SinghamCore] Migrations applied");
        }

        var activePunishments = punishmentManager.loadActivePunishments();
        var openReports = reportManager.loadOpenReports(Integer.MAX_VALUE, 0);
        activePunishments.thenCombine(openReports, (punishmentCount, reports) -> {
            getLogger().info("[SinghamCore] Active punishments loaded: " + punishmentCount);
            getLogger().info("[SinghamCore] Reports loaded: " + reports.size());
            getLogger().info("[SinghamCore] Running in NORMAL mode");
            return null;
        }).exceptionally(ex -> {
            getLogger().warning("[SinghamCore] Startup health check failed: " + ex.getMessage());
            getLogger().warning("[SinghamCore] Running in DEGRADED mode");
            return null;
        });
    }
}
