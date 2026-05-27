package com.friendsmp.singhamcore.managers;

import com.friendsmp.singhamcore.SinghamCorePlugin;
import com.friendsmp.singhamcore.database.DatabaseManager;
import com.friendsmp.singhamcore.models.ReportComment;
import com.friendsmp.singhamcore.models.ReportEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    public CompletableFuture<List<ReportEntry>> loadOpenReports(int limit, int offset) {
        return databaseManager.loadOpenReportsAsync(limit, offset);
    }

    public CompletableFuture<Optional<ReportEntry>> findReportById(long reportId) {
        return databaseManager.findReportByIdAsync(reportId);
    }

    public CompletableFuture<Void> claimReport(long reportId, UUID staffUuid) {
        return databaseManager.claimReportAsync(reportId, staffUuid);
    }

    public CompletableFuture<Void> resolveReport(long reportId, UUID staffUuid) {
        return databaseManager.resolveReportAsync(reportId, staffUuid);
    }

    public CompletableFuture<Void> addComment(long reportId, UUID staffUuid, String note) {
        return databaseManager.saveReportCommentAsync(new ReportComment(0, reportId, staffUuid, note, Instant.now()));
    }

    public CompletableFuture<List<ReportComment>> loadComments(long reportId) {
        return databaseManager.loadReportCommentsAsync(reportId);
    }

    public CompletableFuture<List<ReportEntry>> loadReportsForPlayer(UUID reportedUuid, int limit, int offset) {
        return databaseManager.loadReportsForPlayerAsync(reportedUuid, limit, offset);
    }
}
