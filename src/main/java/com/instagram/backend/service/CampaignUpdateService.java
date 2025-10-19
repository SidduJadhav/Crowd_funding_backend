package com.instagram.backend.service;

import com.instagram.backend.dto.request.CampaignUpdateRequest;
import com.instagram.backend.dto.response.CampaignUpdateResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.CampaignUpdate;
import com.instagram.backend.model.entity.Donation;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.CampaignUpdateRepository;
import com.instagram.backend.repository.jpa.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignUpdateService {

    private final CampaignUpdateRepository campaignUpdateRepository;
    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final NotificationService notificationService;

    @Transactional
    public CampaignUpdateResponse createUpdate(CampaignUpdateRequest request) {
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        // Verify that requester is campaign creator
        if (!campaign.getCreator().getId().equals(request.getCreatorId())) {
            throw new IllegalArgumentException("Only campaign creator can post updates");
        }

        // Validate campaign is active or completed
        if (campaign.getStatus() != Campaign.CampaignStatus.ACTIVE &&
                campaign.getStatus() != Campaign.CampaignStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only post updates for active or completed campaigns");
        }

        CampaignUpdate update = new CampaignUpdate();
        update.setCampaign(campaign);
        update.setTitle(request.getTitle());
        update.setContent(request.getContent());
        update.setImageUrl(request.getImageUrl());
        update.setVideoUrl(request.getVideoUrl());
        update.setIsMilestone(Boolean.TRUE.equals(request.getIsMilestone()));
        update.setMilestoneDescription(request.getMilestoneDescription());

        CampaignUpdate saved = campaignUpdateRepository.save(update);

        // Update campaign counts
        campaign.setUpdatesCount((campaign.getUpdatesCount() != null ? campaign.getUpdatesCount() : 0) + 1);
        if (Boolean.TRUE.equals(request.getIsMilestone())) {
            campaign.setMilestonesCount((campaign.getMilestonesCount() != null ? campaign.getMilestonesCount() : 0) + 1);
        }
        campaignRepository.save(campaign);

        // Notify all donors about the update
        notifyDonors(campaign.getId(), saved.getId(), Boolean.TRUE.equals(request.getIsMilestone()));

        return mapToCampaignUpdateResponse(saved);
    }

    @Transactional
    public CampaignUpdateResponse updateCampaignUpdate(Long updateId, CampaignUpdateRequest request) {
        CampaignUpdate update = campaignUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign update not found"));

        // Verify ownership
        if (!update.getCampaign().getCreator().getId().equals(request.getCreatorId())) {
            throw new IllegalArgumentException("Only creator can edit updates");
        }

        // Track if milestone status changed
        boolean wasMilestone = Boolean.TRUE.equals(update.getIsMilestone());
        boolean isNowMilestone = Boolean.TRUE.equals(request.getIsMilestone());

        update.setTitle(request.getTitle());
        update.setContent(request.getContent());
        update.setImageUrl(request.getImageUrl());
        update.setVideoUrl(request.getVideoUrl());
        update.setIsMilestone(isNowMilestone);
        update.setMilestoneDescription(request.getMilestoneDescription());

        CampaignUpdate updated = campaignUpdateRepository.save(update);

        // Update campaign milestone count if changed
        if (wasMilestone != isNowMilestone) {
            Campaign campaign = update.getCampaign();
            if (isNowMilestone) {
                campaign.setMilestonesCount((campaign.getMilestonesCount() != null ? campaign.getMilestonesCount() : 0) + 1);
            } else {
                campaign.setMilestonesCount(Math.max(0, (campaign.getMilestonesCount() != null ? campaign.getMilestonesCount() : 0) - 1));
            }
            campaignRepository.save(campaign);
        }

        return mapToCampaignUpdateResponse(updated);
    }

    @Transactional
    public void deleteUpdate(Long updateId, Long creatorId) {
        CampaignUpdate update = campaignUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign update not found"));

        // Verify ownership
        if (!update.getCampaign().getCreator().getId().equals(creatorId)) {
            throw new IllegalArgumentException("Only creator can delete updates");
        }

        // Update campaign counts
        Campaign campaign = update.getCampaign();
        campaign.setUpdatesCount(Math.max(0, (campaign.getUpdatesCount() != null ? campaign.getUpdatesCount() : 0) - 1));
        if (Boolean.TRUE.equals(update.getIsMilestone())) {
            campaign.setMilestonesCount(Math.max(0, (campaign.getMilestonesCount() != null ? campaign.getMilestonesCount() : 0) - 1));
        }
        campaignRepository.save(campaign);

        campaignUpdateRepository.delete(update);
    }

    public CampaignUpdateResponse getUpdateById(Long updateId) {
        CampaignUpdate update = campaignUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign update not found"));
        return mapToCampaignUpdateResponse(update);
    }

    public Page<CampaignUpdateResponse> getCampaignUpdates(Long campaignId, Pageable pageable) {
        // Verify campaign exists
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        Page<CampaignUpdate> updates = campaignUpdateRepository
                .findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable);

        return updates.map(this::mapToCampaignUpdateResponse);
    }

    public Page<CampaignUpdateResponse> getMilestones(Long campaignId, Pageable pageable) {
        // Verify campaign exists
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        Page<CampaignUpdate> milestones = campaignUpdateRepository
                .findByCampaignIdAndIsMilestoneTrueOrderByCreatedAtDesc(campaignId, pageable);

        return milestones.map(this::mapToCampaignUpdateResponse);
    }

    public long getUpdateCount(Long campaignId) {
        return campaignUpdateRepository.countByCampaignId(campaignId);
    }

    public long getMilestoneCount(Long campaignId) {
        return campaignUpdateRepository.countByCampaignIdAndIsMilestoneTrue(campaignId);
    }

    @Transactional
    public void deleteAllCampaignUpdates(Long campaignId, Long creatorId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        // Verify ownership
        if (!campaign.getCreator().getId().equals(creatorId)) {
            throw new IllegalArgumentException("Only creator can delete updates");
        }

        // Reset counts
        campaign.setUpdatesCount(0);
        campaign.setMilestonesCount(0);
        campaignRepository.save(campaign);

        campaignUpdateRepository.deleteByCampaignId(campaignId);
    }

    public List<CampaignUpdateResponse> getRecentUpdates(int count) {
        Page<CampaignUpdate> updates = campaignUpdateRepository.findAll(
                Pageable.ofSize(count).withPage(0)
        );
        return updates.stream()
                .map(this::mapToCampaignUpdateResponse)
                .collect(Collectors.toList());
    }

    public Page<CampaignUpdateResponse> getUpdatesByCreator(Long creatorId, Pageable pageable) {
        Page<CampaignUpdate> updates = campaignUpdateRepository.findByCampaignCreatorIdOrderByCreatedAtDesc(creatorId, pageable);
        return updates.map(this::mapToCampaignUpdateResponse);
    }

    private void notifyDonors(Long campaignId, Long updateId, boolean isMilestone) {
        try {
            // Get all unique donors for this campaign
            List<Donation> donations = donationRepository.findByCampaignId(campaignId);

            // Get unique donor IDs from completed donations
            List<Long> donorIds = donations.stream()
                    .filter(d -> d.getPaymentStatus() == Donation.PaymentStatus.COMPLETED)
                    .map(d -> d.getDonor().getId())
                    .distinct()
                    .collect(Collectors.toList());

            // Notify each donor
            for (Long donorId : donorIds) {
                try {
                    if (isMilestone) {
                        notificationService.createCampaignMilestoneNotification(
                                donorId, campaignId, updateId
                        );
                    } else {
                        notificationService.createCampaignUpdateNotification(
                                donorId, campaignId, updateId
                        );
                    }
                } catch (Exception e) {
                    // Log error but continue with other notifications
                    System.err.println("Failed to send notification to donor " + donorId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the update creation
            System.err.println("Failed to send donor notifications: " + e.getMessage());
        }
    }

    private CampaignUpdateResponse mapToCampaignUpdateResponse(CampaignUpdate update) {
        return CampaignUpdateResponse.builder()
                .id(update.getId())
                .campaignId(update.getCampaign().getId())
                .campaignTitle(update.getCampaign().getTitle())
                .creatorId(update.getCampaign().getCreator().getId())
                .creatorUsername(update.getCampaign().getCreator().getUser().getUsername())
                .title(update.getTitle())
                .content(update.getContent())
                .imageUrl(update.getImageUrl())
                .videoUrl(update.getVideoUrl())
                .isMilestone(update.getIsMilestone())
                .milestoneDescription(update.getMilestoneDescription())
                .createdAt(update.getCreatedAt())
                .build();
    }
}