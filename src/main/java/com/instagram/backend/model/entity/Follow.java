package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_follower_following", columnNames = {"follower_id", "following_id"})
        },
        indexes = {
                @Index(name = "idx_follow_follower_id", columnList = "follower_id"),
                @Index(name = "idx_follow_following_id", columnList = "following_id"),
                @Index(name = "idx_follow_status", columnList = "status")
        }
)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private Profile follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private Profile following;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowStatus status = FollowStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        validateFollowRelationship();
    }

    private void validateFollowRelationship() {
        if (follower != null && following != null &&
                follower.getId().equals(following.getId())) {
            throw new IllegalStateException("Users cannot follow themselves");
        }
    }

    public enum FollowStatus {
        ACTIVE,      // Following is confirmed
        PENDING,     // Waiting for approval (private accounts)
        BLOCKED,     // Follower was blocked by following
        MUTED        // Follower muted the following user
    }
}