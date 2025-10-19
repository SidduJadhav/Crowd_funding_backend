package com.instagram.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private String id;
    private Long userId;
    private String username;
    private String profilePictureUrl; // ADDED: Missing field
    private String caption;
    private List<String> mediaUrls;
    private List<String> tags;
    private String location; // ADDED: Missing field
    private Boolean isPublic; // ADDED: Missing field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likesCount; // CHANGED: from int to Integer
    private Integer commentsCount; // CHANGED: from int to Integer
    private Boolean isLiked; // CHANGED: from boolean to Boolean
}