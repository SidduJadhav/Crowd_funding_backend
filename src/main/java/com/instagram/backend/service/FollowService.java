package com.instagram.backend.service;

import com.instagram.backend.dto.response.FollowListResponse;
import com.instagram.backend.dto.response.FollowResponse;
import com.instagram.backend.exception.AlreadyExistsException;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Follow;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.FollowRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Transactional
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        Profile follower = profileRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower profile not found"));
        Profile following = profileRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Following profile not found"));

        // Check if already following or request pending
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new AlreadyExistsException("Follow relationship already exists");
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);

        // Check if target account is private
        if (following.getIsPrivate()) {
            follow.setStatus(Follow.FollowStatus.PENDING);
            followRepository.save(follow);
            // Notify the user about follow request
            notificationService.createFollowRequestNotification(followingId, followerId);
        } else {
            follow.setStatus(Follow.FollowStatus.ACTIVE);
            followRepository.save(follow);
            // Notify the user about new follower
            notificationService.createFollowNotification(followingId, followerId);
        }
    }

    @Transactional
    public void approveFollowRequest(Long followingId, Long followerId) {
        Profile follower = profileRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower profile not found"));
        Profile following = profileRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Following profile not found"));

        Follow follow = followRepository.findByFollowerIdAndFollowingId(follower.getId(), following.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));

        if (follow.getStatus() != Follow.FollowStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be approved");
        }

        follow.setStatus(Follow.FollowStatus.ACTIVE);
        followRepository.save(follow);

        // Notify follower that request was accepted
        notificationService.createFollowAcceptedNotification(followerId, followingId);
    }

    @Transactional
    public void rejectFollowRequest(Long followingId, Long followerId) {
        Profile follower = profileRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Follower profile not found"));
        Profile following = profileRepository.findById(followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Following profile not found"));

        Follow follow = followRepository.findByFollowerIdAndFollowingId(follower.getId(), following.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));

        if (follow.getStatus() != Follow.FollowStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be rejected");
        }

        followRepository.delete(follow);
    }

    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found"));

        followRepository.delete(follow);
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }

        Profile blocker = profileRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocker profile not found"));
        Profile blocked = profileRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked profile not found"));

        // Remove any existing follow relationships
        followRepository.findByFollowerIdAndFollowingId(blockedId, blockerId)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerIdAndFollowingId(blockerId, blockedId)
                .ifPresent(followRepository::delete);

        // Create blocked relationship
        Follow block = new Follow();
        block.setFollower(blocker);
        block.setFollowing(blocked);
        block.setStatus(Follow.FollowStatus.BLOCKED);
        followRepository.save(block);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        Follow block = followRepository.findByFollowerIdAndFollowingIdAndStatus(
                blockerId, blockedId, Follow.FollowStatus.BLOCKED
        ).orElseThrow(() -> new ResourceNotFoundException("Block relationship not found"));

        followRepository.delete(block);
    }

    @Transactional
    public void muteUser(Long userId, Long mutedUserId) {
        if (userId.equals(mutedUserId)) {
            throw new IllegalArgumentException("You cannot mute yourself");
        }

        Follow follow = followRepository.findByFollowerIdAndFollowingId(userId, mutedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found"));

        if (follow.getStatus() == Follow.FollowStatus.BLOCKED) {
            throw new IllegalArgumentException("Cannot mute a blocked user");
        }

        follow.setStatus(Follow.FollowStatus.MUTED);
        followRepository.save(follow);
    }

    @Transactional
    public void unmuteUser(Long userId, Long mutedUserId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingIdAndStatus(
                userId, mutedUserId, Follow.FollowStatus.MUTED
        ).orElseThrow(() -> new ResourceNotFoundException("Mute relationship not found"));

        follow.setStatus(Follow.FollowStatus.ACTIVE);
        followRepository.save(follow);
    }

    public FollowListResponse getFollowers(Long userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<Follow> follows = followRepository.findByFollowingIdAndStatus(
                userId, Follow.FollowStatus.ACTIVE
        );

        List<FollowResponse> followers = follows.stream()
                .map(f -> mapToFollowResponse(f.getFollower()))
                .collect(Collectors.toList());

        FollowListResponse response = new FollowListResponse();
        response.setUsers(followers);
        response.setTotalCount(followers.size());
        return response;
    }

    public FollowListResponse getFollowing(Long userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<Follow> follows = followRepository.findByFollowerIdAndStatus(
                userId, Follow.FollowStatus.ACTIVE
        );

        List<FollowResponse> following = follows.stream()
                .map(f -> mapToFollowResponse(f.getFollowing()))
                .collect(Collectors.toList());

        FollowListResponse response = new FollowListResponse();
        response.setUsers(following);
        response.setTotalCount(following.size());
        return response;
    }

    public FollowListResponse getPendingFollowRequests(Long userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<Follow> pendingRequests = followRepository.findByFollowingIdAndStatus(
                userId, Follow.FollowStatus.PENDING
        );

        List<FollowResponse> requests = pendingRequests.stream()
                .map(f -> mapToFollowResponse(f.getFollower()))
                .collect(Collectors.toList());

        FollowListResponse response = new FollowListResponse();
        response.setUsers(requests);
        response.setTotalCount(requests.size());
        return response;
    }

    public FollowListResponse getBlockedUsers(Long userId) {
        List<Follow> blocked = followRepository.findByFollowerIdAndStatus(
                userId, Follow.FollowStatus.BLOCKED
        );

        List<FollowResponse> blockedUsers = blocked.stream()
                .map(f -> mapToFollowResponse(f.getFollowing()))
                .collect(Collectors.toList());

        FollowListResponse response = new FollowListResponse();
        response.setUsers(blockedUsers);
        response.setTotalCount(blockedUsers.size());
        return response;
    }

    public FollowListResponse getMutedUsers(Long userId) {
        List<Follow> muted = followRepository.findByFollowerIdAndStatus(
                userId, Follow.FollowStatus.MUTED
        );

        List<FollowResponse> mutedUsers = muted.stream()
                .map(f -> mapToFollowResponse(f.getFollowing()))
                .collect(Collectors.toList());

        FollowListResponse response = new FollowListResponse();
        response.setUsers(mutedUsers);
        response.setTotalCount(mutedUsers.size());
        return response;
    }

    public int getFollowersCount(Long userId) {
        return followRepository.countByFollowingIdAndStatus(userId, Follow.FollowStatus.ACTIVE);
    }

    public int getFollowingCount(Long userId) {
        return followRepository.countByFollowerIdAndStatus(userId, Follow.FollowStatus.ACTIVE);
    }

    public int getPendingRequestsCount(Long userId) {
        return followRepository.countByFollowingIdAndStatus(userId, Follow.FollowStatus.PENDING);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                followerId, followingId, Follow.FollowStatus.ACTIVE
        );
    }

    public boolean isFollowRequestPending(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                followerId, followingId, Follow.FollowStatus.PENDING
        );
    }

    public boolean isBlocked(Long userId, Long targetUserId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                userId, targetUserId, Follow.FollowStatus.BLOCKED
        ) || followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                targetUserId, userId, Follow.FollowStatus.BLOCKED
        );
    }

    public boolean isMuted(Long userId, Long targetUserId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                userId, targetUserId, Follow.FollowStatus.MUTED
        );
    }

    private FollowResponse mapToFollowResponse(Profile profile) {
        FollowResponse response = new FollowResponse();
        response.setId(profile.getId());
        response.setUsername(profile.getUser().getUsername());
        response.setName(profile.getName());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());
        response.setIsPrivate(profile.getIsPrivate());
        return response;
    }
}