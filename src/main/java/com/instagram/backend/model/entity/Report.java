package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_content_type", columnList = "content_type"),
        @Index(name = "idx_report_created_at", columnList = "created_at")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private Profile reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private Profile reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    // Content references
    @Column(name = "post_id")
    private String postId;

    @Column(name = "reel_id")
    private String reelId;

    @Column(name = "comment_id")
    private String commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "action_taken", length = 100)
    private String actionTaken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ADD THESE ENUMS
    public enum ContentType {
        POST,
        REEL,
        COMMENT,
        CAMPAIGN,
        PROFILE
    }

    public enum ReportReason {
        SPAM,
        HARASSMENT,
        HATE_SPEECH,
        VIOLENCE,
        NUDITY_OR_SEXUAL_CONTENT,
        SELF_HARM,
        MISINFORMATION,
        SCAM_OR_FRAUD,
        INTELLECTUAL_PROPERTY_VIOLATION,
        INAPPROPRIATE_CONTENT,
        FAKE_CAMPAIGN,
        MISUSE_OF_FUNDS,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        UNDER_REVIEW,
        RESOLVED,
        DISMISSED,
        ESCALATED
    }
}