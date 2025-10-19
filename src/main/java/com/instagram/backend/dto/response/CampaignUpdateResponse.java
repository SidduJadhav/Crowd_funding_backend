package com.instagram.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CampaignUpdateResponse {
    private Long id;
    private Long campaignId;
    private String campaignTitle;
    private Long creatorId;
    private String creatorUsername;
    private String title;
    private String content;
    private String imageUrl;
    private String videoUrl;
    private Boolean isMilestone;
    private String milestoneDescription;
    private LocalDateTime createdAt;
}