package com.instagram.backend.dto.request;

import com.instagram.backend.model.entity.Report;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    private Long reportedByUserId;
    private Long reportedUserId;  // NEW
    private Report.ContentType contentType;  // NEW (POST, REEL, COMMENT, CAMPAIGN, PROFILE)
    private Report.ReportReason reason;  // NEW (enum instead of String)
    private String description;  // NEW
    private String postId;
    private String reelId;
    private String commentId;  // NEW
    private Long campaignId;  // NEW
}