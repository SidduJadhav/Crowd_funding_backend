package com.instagram.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String type;
    private String message;
    private Boolean isRead;

    // Actor information
    private Long actorId;
    private String actorUsername;
    private String actorProfilePicture;

    // Content references
    private String postId;
    private String reelId;
    private String commentId;
    private Long campaignId;

    // Additional details
    private String actionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}