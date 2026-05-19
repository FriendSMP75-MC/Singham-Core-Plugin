package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class ReportEntry {

    private final UUID reporterUuid;
    private final UUID reportedUuid;
    private final String reportedName;
    private final String reason;
    private final Instant createdAt;
    private final String status;

    public ReportEntry(UUID reporterUuid, UUID reportedUuid, String reportedName, String reason, Instant createdAt, String status) {
        this.reporterUuid = reporterUuid;
        this.reportedUuid = reportedUuid;
        this.reportedName = reportedName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = status;
    }

    public UUID getReporterUuid() {
        return reporterUuid;
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
}
