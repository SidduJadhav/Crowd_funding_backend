package com.instagram.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequest {
    
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    private String postId;  // Either postId or reelId must be provided
    private String reelId;
    private Long campaignId;
    // Custom validation method to ensure either postId or reelId is provided
    public boolean isValid() {
        return (postId != null && reelId == null) || (postId == null && reelId != null);
    }
}