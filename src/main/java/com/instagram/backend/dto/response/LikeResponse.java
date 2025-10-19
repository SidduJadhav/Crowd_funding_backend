package com.instagram.backend.dto.response;

import lombok.Data;

@Data
public class LikeResponse {
    private Long id;
    private Long userId;
    private String postId;
    private String reelId;
    private String createdAt;
    private Long campaignId;  // NEW
    private String contentType;
}