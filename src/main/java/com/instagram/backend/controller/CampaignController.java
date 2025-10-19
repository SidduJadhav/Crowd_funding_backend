package com.instagram.backend.controller;

import com.instagram.backend.dto.request.CampaignRequest;
import com.instagram.backend.dto.response.CampaignResponse;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.service.CampaignService;
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
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest request) {
        CampaignResponse campaign = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(campaign);
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> getCampaign(
            @PathVariable Long campaignId,
            @RequestParam(required = false) Long userId) {
        CampaignResponse campaign = campaignService.getCampaignById(campaignId, userId);
        return ResponseEntity.ok(campaign);
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable Long campaignId,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse campaign = campaignService.updateCampaign(campaignId, request);
        return ResponseEntity.ok(campaign);
    }

    @PostMapping("/{campaignId}/publish")
    public ResponseEntity<Void> publishCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long creatorId) {
        campaignService.publishCampaign(campaignId, creatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{campaignId}/pause")
    public ResponseEntity<Void> pauseCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long creatorId) {
        campaignService.pauseCampaign(campaignId, creatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{campaignId}/resume")
    public ResponseEntity<Void> resumeCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long creatorId) {
        campaignService.resumeCampaign(campaignId, creatorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<Page<CampaignResponse>> getActiveCampaigns(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignResponse> campaigns = campaignService.getActiveCampaigns(userId, pageable);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<CampaignResponse>> getCampaignsByCategory(
            @PathVariable String category,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignResponse> campaigns = campaignService.getCampaignsByCategory(category, userId, pageable);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/user/{creatorId}")
    public ResponseEntity<Page<CampaignResponse>> getUserCampaigns(
            @PathVariable Long creatorId,
            @RequestParam(required = false) Long viewerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignResponse> campaigns = campaignService.getUserCampaigns(creatorId, viewerId, pageable);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{campaignId}/progress")
    public ResponseEntity<BigDecimal> getCampaignProgress(@PathVariable Long campaignId) {
        BigDecimal progress = campaignService.getCampaignProgress(campaignId);
        return ResponseEntity.ok(progress);
    }
    @PostMapping("/{campaignId}/approve")
    public ResponseEntity<Void> approveCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long adminId) {
        campaignService.approveCampaign(campaignId, adminId);
        return ResponseEntity.ok().build();
    }
}
