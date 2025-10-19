package com.instagram.backend.service;

import com.instagram.backend.dto.request.DonationRequest;
import com.instagram.backend.dto.response.DonationResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Donation;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.DonationRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final ProfileRepository profileRepository;
    private final CampaignService campaignService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    @Transactional
    public DonationResponse createDonation(DonationRequest request) {
        System.out.println("hi");
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        Profile donor = profileRepository.findById(request.getDonorId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found"));

        // Validate campaign is active
        if (campaign.getStatus() != Campaign.CampaignStatus.ACTIVE) {
            throw new IllegalArgumentException("Campaign is not active for donations");
        }

        // Validate donation amount
        if (request.getAmount().compareTo(new BigDecimal("1")) < 0) {
            throw new IllegalArgumentException("Minimum donation amount is 1");
        }

        // Check if campaign has ended
        if (campaign.getEndDate() != null && campaign.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Campaign has ended");
        }

        // Create donation record
        Donation donation = new Donation();
        donation.setCampaign(campaign);
        donation.setDonor(donor);
        donation.setAmount(request.getAmount());
        donation.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        donation.setIsAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()));
        donation.setMessage(request.getMessage());
        donation.setPaymentMethod(request.getPaymentMethod());
        donation.setPaymentStatus(Donation.PaymentStatus.PENDING);

        Donation savedDonation = donationRepository.save(donation);

        // Process payment
        try {
            String transactionId = paymentService.processPayment(
                    request.getAmount(),
                    donation.getCurrency(),
                    request.getPaymentMethod(),
                    request.getPaymentDetails()
            );

            savedDonation.setTransactionId(transactionId);
            savedDonation.setPaymentStatus(Donation.PaymentStatus.COMPLETED);
            savedDonation = donationRepository.save(savedDonation);

            // Update campaign progress
            campaignService.updateCampaignProgress(campaign.getId(), request.getAmount());

            // Send notification to campaign creator
            if (!Boolean.TRUE.equals(request.getIsAnonymous())) {
                notificationService.createDonationReceivedNotification(
                        campaign.getCreator().getId(),
                        campaign.getId(),
                        donor.getId(),
                        request.getAmount()
                );
            } else {
                notificationService.createAnonymousDonationNotification(
                        campaign.getCreator().getId(),
                        campaign.getId(),
                        request.getAmount()
                );
            }

        } catch (Exception e) {
            savedDonation.setPaymentStatus(Donation.PaymentStatus.FAILED);
            savedDonation.setFailureReason(e.getMessage());
            donationRepository.save(savedDonation);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }

        return mapToDonationResponse(savedDonation);
    }

    @Transactional
    public DonationResponse refundDonation(Long donationId, Long adminId, String reason) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        if (donation.getPaymentStatus() != Donation.PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed donations can be refunded");
        }

        try {
            String refundId = paymentService.processRefund(
                    donation.getTransactionId(),
                    donation.getAmount()
            );

            donation.setRefundId(refundId);
            donation.setRefundAmount(donation.getAmount());
            donation.setPaymentStatus(Donation.PaymentStatus.REFUNDED);
            donation.setUpdatedAt(LocalDateTime.now());

            Donation refunded = donationRepository.save(donation);

            // Update campaign amount - FIXED: Use null-safe subtraction
            Campaign campaign = donation.getCampaign();
            BigDecimal currentAmount = campaign.getCurrentAmount();
            if (currentAmount != null) {
                campaign.setCurrentAmount(currentAmount.subtract(donation.getAmount()));
            }

            // FIXED: Null-safe donor count decrement
            Integer donorCount = campaign.getDonorCount();
            campaign.setDonorCount(Math.max(0, donorCount != null ? donorCount - 1 : 0));
            campaignRepository.save(campaign);

            // Notify donor
            notificationService.createRefundNotification(
                    donation.getDonor().getId(),
                    donationId,
                    reason
            );

            return mapToDonationResponse(refunded);

        } catch (Exception e) {
            throw new RuntimeException("Refund processing failed: " + e.getMessage());
        }
    }

    public DonationResponse getDonationById(Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
        return mapToDonationResponse(donation);
    }

    public Page<DonationResponse> getCampaignDonations(Long campaignId, Pageable pageable) {
        // FIXED: Verify campaign exists first
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        Page<Donation> donations = donationRepository.findByCampaignId(campaignId, pageable);
        return donations.map(this::mapToDonationResponse);
    }

    public Page<DonationResponse> getUserDonations(Long userId, Pageable pageable) {
        // FIXED: Verify user exists first
        if (!profileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        Page<Donation> donations = donationRepository.findByDonorId(userId, pageable);
        return donations.map(this::mapToDonationResponse);
    }

    public BigDecimal getTotalDonationsByCampaign(Long campaignId) {
        // FIXED: Verify campaign exists first
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        BigDecimal total = donationRepository.getTotalDonationsByCampaign(campaignId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Integer getUniqueDonorCount(Long campaignId) {
        // FIXED: Verify campaign exists first
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        Integer count = donationRepository.getUniqueDonorCount(campaignId);
        return count != null ? count : 0;
    }

    public BigDecimal getUserTotalDonations(Long userId) {
        // FIXED: Verify user exists first
        if (!profileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        BigDecimal total = donationRepository.getTotalDonationsByUser(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    // FIXED: Added method to get donations by status
    public Page<DonationResponse> getDonationsByStatus(Long campaignId, Donation.PaymentStatus status, Pageable pageable) {
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }

        Page<Donation> donations = donationRepository.findByCampaignIdAndStatus(campaignId, status, pageable);
        return donations.map(this::mapToDonationResponse);
    }

    private DonationResponse mapToDonationResponse(Donation donation) {
        DonationResponse response = new DonationResponse();
        response.setId(donation.getId());
        response.setCampaignId(donation.getCampaign().getId());
        response.setCampaignTitle(donation.getCampaign().getTitle());

        if (Boolean.TRUE.equals(donation.getIsAnonymous())) {
            response.setDonorId(null);
            response.setDonorUsername("Anonymous");
            response.setDonorName("Anonymous Donor");
            response.setDonorProfilePicture(null);
        } else {
            response.setDonorId(donation.getDonor().getId());
            response.setDonorUsername(donation.getDonor().getUser().getUsername());
            response.setDonorName(donation.getDonor().getName());
            response.setDonorProfilePicture(donation.getDonor().getProfilePictureUrl());
        }

        response.setAmount(donation.getAmount());
        response.setCurrency(donation.getCurrency());
        response.setMessage(donation.getMessage());
        response.setPaymentStatus(donation.getPaymentStatus().name());
        response.setPaymentMethod(donation.getPaymentMethod());
        response.setTransactionId(donation.getTransactionId());
        response.setRefundId(donation.getRefundId());
        response.setRefundAmount(donation.getRefundAmount());
        response.setCreatedAt(donation.getCreatedAt());
        response.setUpdatedAt(donation.getUpdatedAt());
        return response;
    }
}