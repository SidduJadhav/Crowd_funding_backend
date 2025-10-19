package com.instagram.backend.controller;

import com.instagram.backend.dto.request.LikeRequest;
import com.instagram.backend.dto.response.LikeResponse;
import com.instagram.backend.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<LikeResponse> likeContent(@Valid @RequestBody LikeRequest request) {
        LikeResponse like = likeService.likeContent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(like);
    }

    @DeleteMapping
    public ResponseEntity<Void> unlikeContent(@Valid @RequestBody LikeRequest request) {
        likeService.unlikeContent(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> getPostLikesCount(@PathVariable String postId) {
        long count = likeService.getPostLikesCount(postId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/reel/{reelId}/count")
    public ResponseEntity<Long> getReelLikesCount(@PathVariable String reelId) {
        long count = likeService.getReelLikesCount(reelId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/campaign/{campaignId}/count")
    public ResponseEntity<Long> getCampaignLikesCount(@PathVariable Long campaignId) {
        long count = likeService.getCampaignLikesCount(campaignId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/post/{postId}/user/{userId}")
    public ResponseEntity<Boolean> isPostLikedByUser(
            @PathVariable String postId,
            @PathVariable Long userId) {
        boolean liked = likeService.isPostLikedByUser(postId, userId);
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/reel/{reelId}/user/{userId}")
    public ResponseEntity<Boolean> isReelLikedByUser(
            @PathVariable String reelId,
            @PathVariable Long userId) {
        boolean liked = likeService.isReelLikedByUser(reelId, userId);
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/campaign/{campaignId}/user/{userId}")
    public ResponseEntity<Boolean> isCampaignLikedByUser(
            @PathVariable Long campaignId,
            @PathVariable Long userId) {
        boolean liked = likeService.isCampaignLikedByUser(campaignId, userId);
        return ResponseEntity.ok(liked);
    }
}
