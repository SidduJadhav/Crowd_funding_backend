package com.instagram.backend.controller;

import com.instagram.backend.dto.response.FollowListResponse;
import com.instagram.backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{followingId}/follow")
    public ResponseEntity<Void> followUser(
            @PathVariable Long followingId,
            @RequestParam Long followerId) {
        followService.followUser(followerId, followingId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{followingId}/unfollow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable Long followingId,
            @RequestParam Long followerId) {
        followService.unfollowUser(followerId, followingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{followerId}/approve")
    public ResponseEntity<Void> approveFollowRequest(
            @PathVariable Long followerId,
            @RequestParam Long followingId) {
        followService.approveFollowRequest(followingId, followerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{followerId}/reject")
    public ResponseEntity<Void> rejectFollowRequest(
            @PathVariable Long followerId,
            @RequestParam Long followingId) {
        followService.rejectFollowRequest(followingId, followerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{blockedId}/block")
    public ResponseEntity<Void> blockUser(
            @PathVariable Long blockedId,
            @RequestParam Long blockerId) {
        followService.blockUser(blockerId, blockedId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{blockedId}/unblock")
    public ResponseEntity<Void> unblockUser(
            @PathVariable Long blockedId,
            @RequestParam Long blockerId) {
        followService.unblockUser(blockerId, blockedId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<FollowListResponse> getFollowers(@PathVariable Long userId) {
        FollowListResponse followers = followService.getFollowers(userId);
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<FollowListResponse> getFollowing(@PathVariable Long userId) {
        FollowListResponse following = followService.getFollowing(userId);
        return ResponseEntity.ok(following);
    }

    @GetMapping("/{userId}/requests")
    public ResponseEntity<FollowListResponse> getPendingRequests(@PathVariable Long userId) {
        FollowListResponse requests = followService.getPendingFollowRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{userId}/blocked")
    public ResponseEntity<FollowListResponse> getBlockedUsers(@PathVariable Long userId) {
        FollowListResponse blocked = followService.getBlockedUsers(userId);
        return ResponseEntity.ok(blocked);
    }

    @GetMapping("/{followerId}/follows/{followingId}")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long followerId,
            @PathVariable Long followingId) {
        boolean isFollowing = followService.isFollowing(followerId, followingId);
        return ResponseEntity.ok(isFollowing);
    }
}
