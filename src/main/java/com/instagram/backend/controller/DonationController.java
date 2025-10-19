package com.instagram.backend.controller;

import com.instagram.backend.dto.request.DonationRequest;
import com.instagram.backend.dto.response.DonationResponse;
import com.instagram.backend.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public ResponseEntity<DonationResponse> createDonation(@Valid @RequestBody DonationRequest request) {
        DonationResponse donation = donationService.createDonation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(donation);
    }

    @GetMapping("/{donationId}")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable Long donationId) {
        DonationResponse donation = donationService.getDonationById(donationId);
        return ResponseEntity.ok(donation);
    }

    @PostMapping("/{donationId}/refund")
    public ResponseEntity<DonationResponse> refundDonation(
            @PathVariable Long donationId,
            @RequestParam Long adminId,
            @RequestParam String reason) {
        DonationResponse donation = donationService.refundDonation(donationId, adminId, reason);
        return ResponseEntity.ok(donation);
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<Page<DonationResponse>> getCampaignDonations(
            @PathVariable Long campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DonationResponse> donations = donationService.getCampaignDonations(campaignId, pageable);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<DonationResponse>> getUserDonations(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DonationResponse> donations = donationService.getUserDonations(userId, pageable);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/campaign/{campaignId}/total")
    public ResponseEntity<BigDecimal> getTotalDonations(@PathVariable Long campaignId) {
        BigDecimal total = donationService.getTotalDonationsByCampaign(campaignId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/campaign/{campaignId}/donors/count")
    public ResponseEntity<Integer> getUniqueDonorCount(@PathVariable Long campaignId) {
        Integer count = donationService.getUniqueDonorCount(campaignId);
        return ResponseEntity.ok(count);
    }
}
