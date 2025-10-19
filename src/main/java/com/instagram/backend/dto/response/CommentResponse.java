package com.instagram.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private String id;
    private Long userId;
    private String username;
    private String content;
    private String postId;
    private String reelId;
    private String profilePictureUrl;
    private Long campaignId;  // NEW
    private String parentCommentId;  // NEW
    private Integer likeCount;  // NEW
    private Integer replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}