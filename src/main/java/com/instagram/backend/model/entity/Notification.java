package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    public enum NotificationType {
        // Social Notifications
        LIKE_POST, LIKE_REEL, LIKE_CAMPAIGN,
        COMMENT_POST, COMMENT_REEL, COMMENT_CAMPAIGN, COMMENT_REPLY,
        FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED,
        // Crowdfunding Notifications
        DONATION_RECEIVED, CAMPAIGN_GOAL_REACHED, CAMPAIGN_APPROVED, CAMPAIGN_REJECTED,
        CAMPAIGN_UPDATE, CAMPAIGN_MILESTONE,
        // Withdrawal Notifications
        WITHDRAWAL_COMPLETED, WITHDRAWAL_FAILED,
        // Account Notifications
        ACCOUNT_WARNING, ACCOUNT_SUSPENDED,
        // Admin Notifications
        ADMIN_ACTION_REQUIRED,
        // Report Notifications
        REPORT_RESOLVED, REPORT_DISMISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private Profile recipient;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private Profile actor;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "post_id")
    private String postId;

    @Column(name = "reel_id")
    private String reelId;

    @Column(name = "comment_id")
    private String commentId;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // New attributes based on NotificationService usage
    @Column(name = "update_id")
    private Long updateId;

    @Column(name = "withdrawal_id")
    private Long withdrawalId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "donation_id")
    private Long donationId;

    @Column(name = "amount")
    private java.math.BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "reason")
    private String reason;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "suspension_days")
    private Integer suspensionDays;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean getIsRead() {
        return isRead;
    }
}