package com.instagram.backend.service;

import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.dto.request.ReelRequest;
import com.instagram.backend.dto.response.ReelResponse;
import com.instagram.backend.model.document.Reel;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.ReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReelService {

    private final ReelRepository reelRepository;
    private final ProfileRepository profileRepository;
    // REMOVED: LikeService and CommentService dependencies to break circular dependency

    public ReelResponse createReel(ReelRequest reelRequest) {
        Profile profile = profileRepository.findById(reelRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        Reel reel = new Reel();
        reel.setUserId(reelRequest.getUserId());
        reel.setUsername(profile.getUser().getUsername());
        reel.setProfilePictureUrl(profile.getProfilePictureUrl());
        reel.setCaption(reelRequest.getCaption());
        reel.setVideoUrl(reelRequest.getVideoUrl());
        reel.setDuration(reelRequest.getDuration());
        reel.setThumbnailUrl(reelRequest.getThumbnailUrl());
        reel.setTags(reelRequest.getTags());
        reel.setAspectRatio(reelRequest.getAspectRatio());
        reel.setIsPublic(reelRequest.getIsPublic() != null ? reelRequest.getIsPublic() : true);

        Reel savedReel = reelRepository.save(reel);
        return mapToReelResponse(savedReel, reelRequest.getUserId());
    }

    public ReelResponse updateReel(String reelId, ReelRequest reelRequest) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

        if (!reel.getUserId().equals(reelRequest.getUserId())) {
            throw new IllegalArgumentException("You can only update your own reels");
        }

        reel.setCaption(reelRequest.getCaption());
        reel.setThumbnailUrl(reelRequest.getThumbnailUrl());
        reel.setTags(reelRequest.getTags());
        reel.setAspectRatio(reelRequest.getAspectRatio());
        reel.setIsPublic(reelRequest.getIsPublic());
        reel.setUpdatedAt(LocalDateTime.now());

        Reel updatedReel = reelRepository.save(reel);
        return mapToReelResponse(updatedReel, reelRequest.getUserId());
    }

    public Long getReelOwnerId(String reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        return reel.getUserId();
    }

    public ReelResponse getReelById(String reelId, Long userId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

        // Increment view count
        reel.setViewCount(reel.getViewCount() + 1);
        reelRepository.save(reel);

        return mapToReelResponse(reel, userId);
    }

    public void deleteReel(String reelId, Long userId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

        if (!reel.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own reels");
        }

        reelRepository.delete(reel);
    }

    public Page<ReelResponse> getUserReels(Long userId, Long currentUserId, Pageable pageable) {
        Page<Reel> reels = reelRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return reels.map(reel -> mapToReelResponse(reel, currentUserId));
    }

    public void incrementLikeCount(String reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        reel.setLikesCount(reel.getLikesCount() + 1);
        reelRepository.save(reel);
    }

    public void decrementLikeCount(String reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        reel.setLikesCount(Math.max(0, reel.getLikesCount() - 1));
        reelRepository.save(reel);
    }

    public void incrementCommentCount(String reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        reel.setCommentsCount(reel.getCommentsCount() + 1);
        reelRepository.save(reel);
    }

    public void decrementCommentCount(String reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        reel.setCommentsCount(Math.max(0, reel.getCommentsCount() - 1));
        reelRepository.save(reel);
    }

    public void incrementShareCount(String reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));
        reel.setSharesCount(reel.getSharesCount() + 1);
        reelRepository.save(reel);
    }

    private ReelResponse mapToReelResponse(Reel reel, Long currentUserId) {
        ReelResponse response = new ReelResponse();
        response.setId(reel.getId());
        response.setUserId(reel.getUserId());
        response.setUsername(reel.getUsername());
        response.setProfilePictureUrl(reel.getProfilePictureUrl());
        response.setCaption(reel.getCaption());
        response.setVideoUrl(reel.getVideoUrl());
        response.setThumbnailUrl(reel.getThumbnailUrl());
        response.setDuration(reel.getDuration());
        response.setAspectRatio(reel.getAspectRatio());
        response.setTags(reel.getTags());
        response.setViewCount(reel.getViewCount());
        response.setIsPublic(reel.getIsPublic());
        response.setCreatedAt(reel.getCreatedAt());
        response.setUpdatedAt(reel.getUpdatedAt());

        // Use reel's own counts instead of service calls
        response.setLikesCount(reel.getLikesCount());
        response.setCommentsCount(reel.getCommentsCount());
        response.setSharesCount(reel.getSharesCount());

        return response;
    }
}