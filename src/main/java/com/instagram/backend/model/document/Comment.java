package com.instagram.backend.model.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    private Long userId;
    private String username;
    private String profilePictureUrl;
    private String content;
    private String postId;
    private String reelId;
    private Long campaignId;
    private String parentCommentId;
    private Integer likeCount = 0;
    private Integer replyCount = 0;
    private Boolean isDeleted = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;


    public void validateContentReference() {
        int contentRefCount = 0;
        if (postId != null) contentRefCount++;
        if (reelId != null) contentRefCount++;
        if (campaignId != null) contentRefCount++;

        if (contentRefCount != 1) {
            throw new IllegalStateException("Comment must reference exactly one content type (post, reel, or campaign)");
        }
    }
}

