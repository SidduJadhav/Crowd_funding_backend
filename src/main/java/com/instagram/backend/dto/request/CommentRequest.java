package com.instagram.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 2200, message = "Comment must be less than 2200 characters")
    private String content;

    private String postId;
    private String reelId;
    private Long campaignId;  // NEW
    private String parentCommentId;
}