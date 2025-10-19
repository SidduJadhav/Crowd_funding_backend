package com.instagram.backend.service;

import com.instagram.backend.dto.request.CampaignRequest;
import com.instagram.backend.dto.response.CampaignResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.DonationRepository;
import com.instagram.backend.repository.jpa.LikeRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final ProfileRepository profileRepository;
    private final DonationRepository donationRepository;
    private final NotificationService notificationService;
    // REPLACED: LikeService and CommentService with repositories to break circular dependency
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        Profile creator = profileRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Creator profile not found"));

        // Validate campaign data
        validateCampaignRequest(request);

        Campaign campaign = new Campaign();
        campaign.setCreator(creator);
        campaign.setTitle(request.getTitle());
        campaign.setDescription(request.getDescription());
        campaign.setGoalAmount(request.getGoalAmount());
        campaign.setCurrency(request.getCurrency());
        campaign.setCategory(request.getCategory());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setImageUrl(request.getImageUrl());
        campaign.setVideoUrl(request.getVideoUrl());
        campaign.setBeneficiaryName(request.getBeneficiaryName());
        campaign.setStatus(Campaign.CampaignStatus.DRAFT);

        Campaign savedCampaign = campaignRepository.save(campaign);
        return mapToCampaignResponse(savedCampaign, request.getCreatorId());
    }

    @Transactional
    public CampaignResponse updateCampaign(Long campaignId, CampaignRequest request) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        // Only creator can update
        if (!campaign.getCreator().getId().equals(request.getCreatorId())) {
            throw new IllegalArgumentException("Only campaign creator can update");
        }

        // Cannot update if campaign is completed or cancelled
        if (campaign.getStatus() == Campaign.CampaignStatus.COMPLETED ||
                campaign.getStatus() == Campaign.CampaignStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update completed or cancelled campaign");
        }

        campaign.setTitle(request.getTitle());
        campaign.setDescription(request.getDescription());
        campaign.setEndDate(request.getEndDate());
        campaign.setImageUrl(request.getImageUrl());
        campaign.setVideoUrl(request.getVideoUrl());

        Campaign updated = campaignRepository.save(campaign);
        return mapToCampaignResponse(updated, request.getCreatorId());
    }

    @Transactional
    public void publishCampaign(Long campaignId, Long creatorId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(creatorId)) {
            throw new IllegalArgumentException("Only creator can publish campaign");
        }

        if (campaign.getStatus() != Campaign.CampaignStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft campaigns can be published");
        }

        campaign.setStatus(Campaign.CampaignStatus.UNDER_REVIEW);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void approveCampaign(Long campaignId, Long adminId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (campaign.getStatus() != Campaign.CampaignStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Only campaigns under review can be approved");
        }

        campaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        campaign.setIsVerified(true);
        campaign.setStartDate(LocalDateTime.now());
        campaignRepository.save(campaign);

        // Notify creator
        notificationService.createCampaignApprovedNotification(
                campaign.getCreator().getId(), campaignId
        );
    }

    @Transactional
    public void rejectCampaign(Long campaignId, Long adminId, String reason) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        campaign.setStatus(Campaign.CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);

        // Notify creator with reason
        notificationService.createCampaignRejectedNotification(
                campaign.getCreator().getId(), campaignId, reason
        );
    }

    @Transactional
    public void pauseCampaign(Long campaignId, Long creatorId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(creatorId)) {
            throw new IllegalArgumentException("Only creator can pause campaign");
        }

        if (campaign.getStatus() != Campaign.CampaignStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active campaigns can be paused");
        }

        campaign.setStatus(Campaign.CampaignStatus.PAUSED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void resumeCampaign(Long campaignId, Long creatorId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (!campaign.getCreator().getId().equals(creatorId)) {
            throw new IllegalArgumentException("Only creator can resume campaign");
        }

        if (campaign.getStatus() != Campaign.CampaignStatus.PAUSED) {
            throw new IllegalArgumentException("Only paused campaigns can be resumed");
        }

        campaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void updateCampaignProgress(Long campaignId, BigDecimal amount) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        BigDecimal newAmount = campaign.getCurrentAmount().add(amount);
        campaign.setCurrentAmount(newAmount);
        campaign.setDonorCount(campaign.getDonorCount() + 1);

        // Check if goal reached
        if (newAmount.compareTo(campaign.getGoalAmount()) >= 0) {
            campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
            notificationService.createCampaignGoalReachedNotification(
                    campaign.getCreator().getId(), campaignId
            );
        }

        campaignRepository.save(campaign);
    }

    public CampaignResponse getCampaignById(Long campaignId, Long userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        return mapToCampaignResponse(campaign, userId);
    }

    public Page<CampaignResponse> getActiveCampaigns(Long userId, Pageable pageable) {
        Page<Campaign> campaigns = campaignRepository.findByStatus(
                Campaign.CampaignStatus.ACTIVE, pageable
        );
        return campaigns.map(c -> mapToCampaignResponse(c, userId));
    }

    public Page<CampaignResponse> getCampaignsByCategory(
            String category, Long userId, Pageable pageable
    ) {
        Page<Campaign> campaigns = campaignRepository.findByCategory(category, pageable);
        return campaigns.map(c -> mapToCampaignResponse(c, userId));
    }

    public Page<CampaignResponse> getUserCampaigns(
            Long creatorId, Long viewerId, Pageable pageable
    ) {
        Page<Campaign> campaigns = campaignRepository.findByCreatorId(creatorId, pageable);
        return campaigns.map(c -> mapToCampaignResponse(c, viewerId));
    }

    public Page<CampaignResponse> getTrendingCampaigns(Long userId, Pageable pageable) {
        List<Campaign> campaigns = campaignRepository.findActiveCampaigns(pageable);
        // Sort by donation velocity (recent donations)
        return Page.empty(); // Implement sorting logic
    }

    public BigDecimal getCampaignProgress(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (campaign.getGoalAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return campaign.getCurrentAmount()
                .divide(campaign.getGoalAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private void validateCampaignRequest(CampaignRequest request) {
        if (request.getGoalAmount().compareTo(new BigDecimal("100")) < 0) {
            throw new IllegalArgumentException("Goal amount must be at least 100");
        }

        if (request.getGoalAmount().compareTo(new BigDecimal("10000000")) > 0) {
            throw new IllegalArgumentException("Goal amount cannot exceed 10,000,000");
        }

        if (request.getEndDate() != null &&
                request.getEndDate().isBefore(LocalDateTime.now().plusDays(1))) {
            throw new IllegalArgumentException("End date must be at least 1 day in future");
        }

        if (request.getEndDate() != null &&
                request.getEndDate().isAfter(LocalDateTime.now().plusDays(90))) {
            throw new IllegalArgumentException("Campaign cannot exceed 90 days");
        }
    }

    private CampaignResponse mapToCampaignResponse(Campaign campaign, Long viewerId) {
        CampaignResponse response = new CampaignResponse();
        response.setId(campaign.getId());
        response.setCreatorId(campaign.getCreator().getId());
        response.setCreatorUsername(campaign.getCreator().getUser().getUsername());
        response.setCreatorName(campaign.getCreator().getName());
        response.setCreatorProfilePicture(campaign.getCreator().getProfilePictureUrl());
        response.setTitle(campaign.getTitle());
        response.setDescription(campaign.getDescription());
        response.setGoalAmount(campaign.getGoalAmount());
        response.setCurrentAmount(campaign.getCurrentAmount());
        response.setCurrency(campaign.getCurrency());
        response.setCategory(campaign.getCategory());
        response.setStatus(campaign.getStatus().name());
        response.setStartDate(campaign.getStartDate());
        response.setEndDate(campaign.getEndDate());
        response.setImageUrl(campaign.getImageUrl());
        response.setVideoUrl(campaign.getVideoUrl());
        response.setIsVerified(campaign.getIsVerified());
        response.setBeneficiaryName(campaign.getBeneficiaryName());
        response.setDonorCount(campaign.getDonorCount());

        // Use repositories directly instead of service calls to avoid circular dependency
        response.setLikesCount(likeRepository.countByCampaignId(campaign.getId()));
        response.setCommentsCount(commentRepository.countByCampaignIdAndIsDeletedFalse(campaign.getId()));
        response.setLiked(viewerId != null && likeRepository.existsByUserIdAndCampaignId(viewerId, campaign.getId()));

        response.setProgressPercentage(getCampaignProgress(campaign.getId()));
        response.setCreatedAt(campaign.getCreatedAt());
        response.setUpdatedAt(campaign.getUpdatedAt());
        return response;
    }
}