package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_post", columnNames = {"user_id", "post_id"}),
                @UniqueConstraint(name = "uk_user_reel", columnNames = {"user_id", "reel_id"}),
                @UniqueConstraint(name = "uk_user_campaign", columnNames = {"user_id", "campaign_id"})
        },
        indexes = {
                @Index(name = "idx_like_post_id", columnList = "post_id"),
                @Index(name = "idx_like_reel_id", columnList = "reel_id"),
                @Index(name = "idx_like_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_like_user_id", columnList = "user_id")
        }
)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    // References to MongoDB documents (stored as String IDs)
    @Column(name = "post_id")
    private String postId;

    @Column(name = "reel_id")
    private String reelId;

    // Reference to SQL entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        validateContentReference();
    }

    private void validateContentReference() {
        int count = 0;
        if (postId != null) count++;
        if (reelId != null) count++;
        if (campaign != null) count++;

        if (count != 1) {
            throw new IllegalStateException(
                    "Exactly one of postId, reelId, or campaign must be set"
            );
        }

        // Set content type based on which field is populated
        if (postId != null) contentType = ContentType.POST;
        else if (reelId != null) contentType = ContentType.REEL;
        else contentType = ContentType.CAMPAIGN;
    }

    public enum ContentType {
        POST,
        REEL,
        CAMPAIGN
    }
}