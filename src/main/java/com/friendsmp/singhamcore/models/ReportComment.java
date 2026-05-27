package com.friendsmp.singhamcore.models;

import java.time.Instant;
import java.util.UUID;

public class ReportComment {

    private final long commentId;
    private final long reportId;
    private final UUID staffUuid;
    private final String note;
    private final Instant createdAt;

    public ReportComment(long commentId, long reportId, UUID staffUuid, String note, Instant createdAt) {
        this.commentId = commentId;
        this.reportId = reportId;
        this.staffUuid = staffUuid;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getCommentId() {
        return commentId;
    }

    public long getReportId() {
        return reportId;
    }

    public UUID getStaffUuid() {
        return staffUuid;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
