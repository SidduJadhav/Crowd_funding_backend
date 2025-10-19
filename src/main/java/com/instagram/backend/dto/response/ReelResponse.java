package com.instagram.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReelResponse {
    private String id;
    private Long userId;
    private String username;
    private String profilePictureUrl; // ADDED: Missing field
    private String caption;
    private String videoUrl;
    private String thumbnailUrl; // ADDED: Missing field
    private Integer duration; // ADDED: Missing field
    private String aspectRatio; // ADDED: Missing field
    private List<String> tags;
    private Integer viewCount; // CHANGED: from int to Integer
    private Boolean isPublic; // ADDED: Missing field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // ADDED: Missing field
    private Integer likesCount; // CHANGED: from int to Integer
    private Integer commentsCount; // CHANGED: from int to Integer
    private Integer sharesCount; // ADDED: Missing field
    private Boolean isLiked; // CHANGED: from boolean to Boolean
}