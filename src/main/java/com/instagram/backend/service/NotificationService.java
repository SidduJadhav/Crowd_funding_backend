package com.instagram.backend.service;

import com.instagram.backend.dto.response.NotificationResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Notification;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.NotificationRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.PostRepository;
import com.instagram.backend.repository.mongo.ReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final CampaignRepository campaignRepository;

    // ==================== READ NOTIFICATIONS ====================

    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::mapToNotificationResponse);
    }

    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to notification");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndIsReadFalse(userId);

        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        notificationRepository.delete(notification);
    }

    // ==================== SOCIAL NOTIFICATIONS ====================

    @Transactional
    public void createLikeNotification(
            String postId, String reelId, Long campaignId, Long actorId
    ) {
        Long recipientId = getContentOwnerId(postId, reelId, campaignId);

        // Don't notify if user likes their own content
        if (recipientId.equals(actorId)) {
            return;
        }

        Profile actor = getProfile(actorId);
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setPostId(postId);
        notification.setReelId(reelId);

        if (campaignId != null) {
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
            notification.setCampaign(campaign);
            notification.setType(Notification.NotificationType.LIKE_CAMPAIGN);
            notification.setMessage(actor.getUser().getUsername() + " liked your campaign");
        } else if (postId != null) {
            notification.setType(Notification.NotificationType.LIKE_POST);
            notification.setMessage(actor.getUser().getUsername() + " liked your post");
        } else {
            notification.setType(Notification.NotificationType.LIKE_REEL);
            notification.setMessage(actor.getUser().getUsername() + " liked your reel");
        }

        notificationRepository.save(notification);
    }

    @Transactional
    public void createCommentNotification(
            String postId, String reelId, Long campaignId, Long actorId, boolean isReply
    ) {
        Long recipientId = getContentOwnerId(postId, reelId, campaignId);

        // Don't notify if user comments on their own content
        if (recipientId.equals(actorId) && !isReply) {
            return;
        }

        Profile actor = getProfile(actorId);
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setPostId(postId);
        notification.setReelId(reelId);

        if (isReply) {
            notification.setType(Notification.NotificationType.COMMENT_REPLY);
            notification.setMessage(actor.getUser().getUsername() + " replied to your comment");
        } else if (campaignId != null) {
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
            notification.setCampaign(campaign);
            notification.setType(Notification.NotificationType.COMMENT_CAMPAIGN);
            notification.setMessage(actor.getUser().getUsername() + " commented on your campaign");
        } else if (postId != null) {
            notification.setType(Notification.NotificationType.COMMENT_POST);
            notification.setMessage(actor.getUser().getUsername() + " commented on your post");
        } else {
            notification.setType(Notification.NotificationType.COMMENT_REEL);
            notification.setMessage(actor.getUser().getUsername() + " commented on your reel");
        }

        notificationRepository.save(notification);
    }

    @Transactional
    public void createFollowNotification(Long recipientId, Long actorId) {
        Profile actor = getProfile(actorId);
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(Notification.NotificationType.FOLLOW);
        notification.setMessage(actor.getUser().getUsername() + " started following you");

        notificationRepository.save(notification);
    }

    // ==================== CROWDFUNDING NOTIFICATIONS ====================

    @Transactional
    public void createDonationReceivedNotification(
            Long recipientId, Long campaignId, Long donorId, BigDecimal amount
    ) {
        Profile donor = getProfile(donorId);
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(donor);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.DONATION_RECEIVED);
        notification.setMessage(String.format(
                "%s donated %s %s to your campaign",
                donor.getUser().getUsername(), amount, campaign.getCurrency()
        ));

        notificationRepository.save(notification);
    }

    @Transactional
    public void createAnonymousDonationNotification(
            Long recipientId, Long campaignId, BigDecimal amount
    ) {
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.DONATION_RECEIVED);
        notification.setMessage(String.format(
                "Someone donated %s %s to your campaign anonymously",
                amount, campaign.getCurrency()
        ));

        notificationRepository.save(notification);
    }

    @Transactional
    public void createCampaignGoalReachedNotification(Long recipientId, Long campaignId) {
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.CAMPAIGN_GOAL_REACHED);
        notification.setMessage("Congratulations! Your campaign reached its goal");

        notificationRepository.save(notification);
    }

    @Transactional
    public void createCampaignApprovedNotification(Long recipientId, Long campaignId) {
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.CAMPAIGN_APPROVED);
        notification.setMessage("Your campaign has been approved and is now live");

        notificationRepository.save(notification);
    }

    @Transactional
    public void createCampaignRejectedNotification(
            Long recipientId, Long campaignId, String reason
    ) {
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.CAMPAIGN_REJECTED);
        notification.setMessage("Your campaign was rejected. Reason: " + reason);

        notificationRepository.save(notification);
    }

    // ==================== WITHDRAWAL NOTIFICATIONS ====================

    @Transactional
    public void createWithdrawalRequestNotification(Long withdrawalId) {
        // Notify admins - implement admin notification logic




    }

    @Transactional
    public void createWithdrawalApprovedNotification(Long recipientId, Long withdrawalId) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.WITHDRAWAL_COMPLETED);
        notification.setMessage("Your withdrawal request has been approved");

        notificationRepository.save(notification);
    }

    @Transactional
    public void createWithdrawalRejectedNotification(
            Long recipientId, Long withdrawalId, String reason
    ) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.WITHDRAWAL_FAILED);
        notification.setMessage("Withdrawal rejected: " + reason);

        notificationRepository.save(notification);
    }

    @Transactional
    public void createWithdrawalCompletedNotification(Long recipientId, Long withdrawalId) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.WITHDRAWAL_COMPLETED);
        notification.setMessage("Your withdrawal has been completed successfully");

        notificationRepository.save(notification);
    }

    @Transactional
    public void createWithdrawalFailedNotification(
            Long recipientId, Long withdrawalId, String reason
    ) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.WITHDRAWAL_FAILED);
        notification.setMessage("Withdrawal failed: " + reason);

        notificationRepository.save(notification);
    }

    // ==================== OTHER NOTIFICATIONS ====================

    @Transactional
    public void createBankAccountVerifiedNotification(Long recipientId, Long accountId) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.ACCOUNT_WARNING);
        notification.setMessage("Your bank account has been verified");

        notificationRepository.save(notification);
    }

    @Transactional
    public void createBankAccountVerificationRequestNotification(Long accountId) {
        // Notify admins
    }

    @Transactional
    public void createRefundNotification(Long recipientId, Long donationId, String reason) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.ACCOUNT_WARNING);
        notification.setMessage("Your donation has been refunded. Reason: " + reason);

        notificationRepository.save(notification);
    }

    // ==================== HELPER METHODS ====================

    private Long getContentOwnerId(String postId, String reelId, Long campaignId) {
        if (postId != null) {
            return postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"))
                    .getUserId();
        } else if (reelId != null) {
            return reelRepository.findById(reelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reel not found"))
                    .getUserId();
        } else if (campaignId != null) {
            return campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"))
                    .getCreator().getId();
        }
        throw new IllegalArgumentException("No content ID provided");
    }

    private Profile getProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }

    private Campaign getCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType().name());
        response.setMessage(notification.getMessage());
        response.setIsRead(notification.getIsRead());

        if (notification.getActor() != null) {
            response.setActorId(notification.getActor().getId());
            response.setActorUsername(notification.getActor().getUser().getUsername());
            response.setActorProfilePicture(notification.getActor().getProfilePictureUrl());
        }

        response.setPostId(notification.getPostId());
        response.setReelId(notification.getReelId());
        response.setCommentId(notification.getCommentId());

        if (notification.getCampaign() != null) {
            response.setCampaignId(notification.getCampaign().getId());
        }

        response.setActionUrl(notification.getActionUrl());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());

        return response;
    }
    // Add these methods to NotificationService.java

// ==================== CAMPAIGN UPDATE NOTIFICATIONS ====================

    @Transactional
    public void createCampaignUpdateNotification(Long recipientId, Long campaignId, Long updateId) {
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.CAMPAIGN_UPDATE);
        notification.setMessage("New update posted for campaign: " + campaign.getTitle());
        notification.setActionUrl("/campaigns/" + campaignId + "/updates/" + updateId);

        notificationRepository.save(notification);
    }

    @Transactional
    public void createCampaignMilestoneNotification(Long recipientId, Long campaignId, Long updateId) {
        Profile recipient = getProfile(recipientId);
        Campaign campaign = getCampaign(campaignId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setCampaign(campaign);
        notification.setType(Notification.NotificationType.CAMPAIGN_MILESTONE);
        notification.setMessage("Campaign milestone reached: " + campaign.getTitle());
        notification.setActionUrl("/campaigns/" + campaignId + "/updates/" + updateId);

        notificationRepository.save(notification);
    }

// ==================== FOLLOW NOTIFICATIONS ====================

    @Transactional
    public void createFollowRequestNotification(Long recipientId, Long actorId) {
        Profile actor = getProfile(actorId);
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(Notification.NotificationType.FOLLOW_REQUEST);
        notification.setMessage(actor.getUser().getUsername() + " requested to follow you");

        notificationRepository.save(notification);
    }

    @Transactional
    public void createFollowAcceptedNotification(Long recipientId, Long actorId) {
        Profile actor = getProfile(actorId);
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(Notification.NotificationType.FOLLOW_ACCEPTED);
        notification.setMessage(actor.getUser().getUsername() + " accepted your follow request");

        notificationRepository.save(notification);
    }

// ==================== REPORT NOTIFICATIONS ====================

    @Transactional
    public void createNewReportNotification(Long reportId) {
        // This would notify all admins - implementation depends on admin management system
        // For now, create a system notification
        List<Profile> admins = profileRepository.findByRole("ADMIN");

        for (Profile admin : admins) {
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setType(Notification.NotificationType.ADMIN_ACTION_REQUIRED);
            notification.setMessage("New report requires review");
            notification.setActionUrl("/admin/reports/" + reportId);

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void createReportResolvedNotification(Long recipientId, Long reportId, String action) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.REPORT_RESOLVED);
        notification.setMessage("Your report has been resolved. Action taken: " + action);
        notification.setActionUrl("/reports/" + reportId);

        notificationRepository.save(notification);
    }

    @Transactional
    public void createReportDismissedNotification(Long recipientId, Long reportId) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.REPORT_DISMISSED);
        notification.setMessage("Your report has been reviewed and dismissed");
        notification.setActionUrl("/reports/" + reportId);

        notificationRepository.save(notification);
    }

    @Transactional
    public void createReportEscalatedNotification(Long reportId) {
        // Notify senior admins
        List<Profile> seniorAdmins = profileRepository.findByRole("SENIOR_ADMIN");

        for (Profile admin : seniorAdmins) {
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setType(Notification.NotificationType.ADMIN_ACTION_REQUIRED);
            notification.setMessage("Report escalated for senior review");
            notification.setActionUrl("/admin/reports/" + reportId);

            notificationRepository.save(notification);
        }
    }

// ==================== MODERATION NOTIFICATIONS ====================

    @Transactional
    public void createContentWarningNotification(Long recipientId, String contentType, String reason) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.ACCOUNT_WARNING);
        notification.setMessage(String.format(
                "Your %s violates community guidelines. Reason: %s",
                contentType.toLowerCase(), reason
        ));

        notificationRepository.save(notification);
    }

    @Transactional
    public void createAccountSuspensionNotification(Long recipientId, Integer days, String reason) {
        Profile recipient = getProfile(recipientId);

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(Notification.NotificationType.ACCOUNT_SUSPENDED);
        notification.setMessage(String.format(
                "Your account has been suspended for %d days. Reason: %s",
                days, reason
        ));

        notificationRepository.save(notification);
    }
}