package com.instagram.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class CampaignUpdateRequest {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotNull(message = "Creator ID is required")
    private Long creatorId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 5000, message = "Content must be between 10 and 5000 characters")
    private String content;

    private String imageUrl;

    private String videoUrl;

    @NotNull(message = "Milestone flag is required")
    private Boolean isMilestone = false;

    @Size(max = 500, message = "Milestone description must not exceed 500 characters")
    private String milestoneDescription;
}