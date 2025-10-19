package com.instagram.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportResponse {
    private Long id;
    private Long reportedByUserId;
    private String reportedByUsername;
    private String contentType;  // NEW
    private String reason;
    private String description;  // NEW
    private String status;  // NEW
    private String postId;
    private String reelId;
    private String commentId;  // NEW
    private Long campaignId;  // NEW
    private Long reportedUserId;  // NEW
    private String reportedUsername;  // NEW
    private String actionTaken;  // NEW
    private String reviewNotes;  // NEW
    private LocalDateTime reviewedAt;  // NEW
    private LocalDateTime createdAt;
}