package com.instagram.backend.controller;

import com.instagram.backend.dto.request.CampaignUpdateRequest;
import com.instagram.backend.dto.response.CampaignUpdateResponse;
import com.instagram.backend.service.CampaignUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaign-updates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampaignUpdateController {

    private final CampaignUpdateService campaignUpdateService;

    @PostMapping
    public ResponseEntity<CampaignUpdateResponse> createUpdate(@Valid @RequestBody CampaignUpdateRequest request) {
        CampaignUpdateResponse update = campaignUpdateService.createUpdate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(update);
    }

    @GetMapping("/{updateId}")
    public ResponseEntity<CampaignUpdateResponse> getUpdate(@PathVariable Long updateId) {
        CampaignUpdateResponse update = campaignUpdateService.getUpdateById(updateId);
        return ResponseEntity.ok(update);
    }

    @PutMapping("/{updateId}")
    public ResponseEntity<CampaignUpdateResponse> updateCampaignUpdate(
            @PathVariable Long updateId,
            @Valid @RequestBody CampaignUpdateRequest request) {
        CampaignUpdateResponse update = campaignUpdateService.updateCampaignUpdate(updateId, request);
        return ResponseEntity.ok(update);
    }

    @DeleteMapping("/{updateId}")
    public ResponseEntity<Void> deleteUpdate(
            @PathVariable Long updateId,
            @RequestParam Long creatorId) {
        campaignUpdateService.deleteUpdate(updateId, creatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<Page<CampaignUpdateResponse>> getCampaignUpdates(
            @PathVariable Long campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignUpdateResponse> updates = campaignUpdateService.getCampaignUpdates(campaignId, pageable);
        return ResponseEntity.ok(updates);
    }

    @GetMapping("/campaign/{campaignId}/milestones")
    public ResponseEntity<Page<CampaignUpdateResponse>> getCampaignMilestones(
            @PathVariable Long campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignUpdateResponse> milestones = campaignUpdateService.getMilestones(campaignId, pageable);
        return ResponseEntity.ok(milestones);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<CampaignUpdateResponse>> getRecentUpdates(
            @RequestParam(defaultValue = "10") int count) {
        List<CampaignUpdateResponse> updates = campaignUpdateService.getRecentUpdates(count);
        return ResponseEntity.ok(updates);
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<Page<CampaignUpdateResponse>> getUpdatesByCreator(
            @PathVariable Long creatorId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignUpdateResponse> updates = campaignUpdateService.getUpdatesByCreator(creatorId, pageable);
        return ResponseEntity.ok(updates);
    }
}