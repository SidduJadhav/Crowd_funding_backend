package com.instagram.backend.controller;

import com.instagram.backend.dto.request.ReelRequest;
import com.instagram.backend.dto.response.ReelResponse;
import com.instagram.backend.service.ReelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reels")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReelController {

    private final ReelService reelService;

    @PostMapping
    public ResponseEntity<ReelResponse> createReel(@Valid @RequestBody ReelRequest request) {
        ReelResponse reel = reelService.createReel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reel);
    }

    @GetMapping("/{reelId}")
    public ResponseEntity<ReelResponse> getReel(
            @PathVariable String reelId,
            @RequestParam(required = false) Long userId) {
        ReelResponse reel = reelService.getReelById(reelId, userId);
        return ResponseEntity.ok(reel);
    }

    @PutMapping("/{reelId}")
    public ResponseEntity<ReelResponse> updateReel(
            @PathVariable String reelId,
            @Valid @RequestBody ReelRequest request) {
        ReelResponse reel = reelService.updateReel(reelId, request);
        return ResponseEntity.ok(reel);
    }

    @DeleteMapping("/{reelId}")
    public ResponseEntity<Void> deleteReel(
            @PathVariable String reelId,
            @RequestParam Long userId) {
        reelService.deleteReel(reelId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReelResponse>> getUserReels(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReelResponse> reels = reelService.getUserReels(userId, currentUserId, pageable);
        return ResponseEntity.ok(reels);
    }
}
