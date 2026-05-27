package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class ReportEntry {

    private final long reportId;
    private final UUID reporterUuid;
    private final UUID reportedUuid;
    private final String reportedName;
    private final String reason;
    private final Instant createdAt;
    private final String status;
    private final UUID claimedBy;
    private final Instant claimedAt;
    private final UUID resolvedBy;
    private final Instant resolvedAt;

    public ReportEntry(long reportId, UUID reporterUuid, UUID reportedUuid, String reportedName, String reason, Instant createdAt, String status,
                       UUID claimedBy, Instant claimedAt, UUID resolvedBy, Instant resolvedAt) {
        this.reportId = reportId;
        this.reporterUuid = reporterUuid;
        this.reportedUuid = reportedUuid;
        this.reportedName = reportedName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = status;
        this.claimedBy = claimedBy;
        this.claimedAt = claimedAt;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public ReportEntry(UUID reporterUuid, UUID reportedUuid, String reportedName, String reason, Instant createdAt, String status) {
        this(0L, reporterUuid, reportedUuid, reportedName, reason, createdAt, status, null, null, null, null);
    }

    public UUID getReporterUuid() {
        return reporterUuid;
    }

    public long getReportId() {
        return reportId;
    }

    public UUID getReportedUuid() {
        return reportedUuid;
    }

    public String getReportedName() {
        return reportedName;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public UUID getClaimedBy() {
        return claimedBy;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
