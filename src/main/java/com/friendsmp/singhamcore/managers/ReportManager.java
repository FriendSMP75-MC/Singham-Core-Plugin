package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.ReportEntry;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportManager {

    private final DatabaseManager databaseManager;

    public ReportManager(SinghamCorePlugin plugin, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> submitReport(ReportEntry entry) {
        return databaseManager.saveReportAsync(entry);
    }
}
