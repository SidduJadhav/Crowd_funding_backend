package com.instagram.backend.service;

import com.instagram.backend.dto.request.LikeRequest;
import com.instagram.backend.dto.response.LikeResponse;
import com.instagram.backend.exception.AlreadyExistsException;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Like;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.LikeRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.PostRepository;
import com.instagram.backend.repository.mongo.ReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProfileRepository profileRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;

    @Transactional
    public LikeResponse likeContent(LikeRequest request) {
        Profile user = profileRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate exactly one content ID is provided
        System.out.println(request.getPostId() + " " + request.getReelId() + " " + request.getCampaignId() );
        int contentCount = 0;
        if (request.getPostId() != null) contentCount++;
        if (request.getReelId() != null) contentCount++;
        if (request.getCampaignId() != null) contentCount++;

        if (contentCount != 1) {
            throw new IllegalArgumentException(
                    "Exactly one of postId, reelId, or campaignId must be provided"
            );
        }

        Like like = new Like();
        like.setUser(user);

        // Handle Post Like
        if (request.getPostId() != null) {
            // Find the post directly from its repository to avoid circular dependency
            com.instagram.backend.model.document.Post post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            // Prevent self-like by checking the post's userId
            if (post.getUserId().equals(request.getUserId())) {
                throw new IllegalArgumentException("Cannot like your own post");
            }

            if (likeRepository.existsByUserIdAndPostId(user.getId(), request.getPostId())) {
                throw new AlreadyExistsException("Post already liked");
            }

            like.setPostId(request.getPostId());
            like.setContentType(Like.ContentType.POST);

            Like saved = likeRepository.save(like);
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);

            notificationService.createLikeNotification(
                    request.getPostId(), null, null, request.getUserId()
            );

            return mapToLikeResponse(saved);
        }

        // Handle Reel Like
        else if (request.getReelId() != null) {
            // Find the reel directly from its repository to avoid circular dependency
            com.instagram.backend.model.document.Reel reel = reelRepository.findById(request.getReelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reel not found"));

            // Prevent self-like by checking the reel's userId
            if (reel.getUserId().equals(request.getUserId())) {
                throw new IllegalArgumentException("Cannot like your own reel");
            }

            if (likeRepository.existsByUserIdAndReelId(user.getId(), request.getReelId())) {
                throw new AlreadyExistsException("Reel already liked");
            }

            like.setReelId(request.getReelId());
            like.setContentType(Like.ContentType.REEL);
            Like saved = likeRepository.save(like);

            notificationService.createLikeNotification(
                    null, request.getReelId(), null, request.getUserId()
            );

            return mapToLikeResponse(saved);
        }

        // Handle Campaign Like
        else {
            Campaign campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

            // Prevent self-like
            if (campaign.getCreator().getId().equals(request.getUserId())) {
                throw new IllegalArgumentException("Cannot like your own campaign");
            }

            if (likeRepository.existsByUserIdAndCampaignId(
                    user.getId(), request.getCampaignId()
            )) {
                throw new AlreadyExistsException("Campaign already liked");
            }

            like.setCampaign(campaign);
            like.setContentType(Like.ContentType.CAMPAIGN);
            Like saved = likeRepository.save(like);

            notificationService.createLikeNotification(
                    null, null, request.getCampaignId(), request.getUserId()
            );

            return mapToLikeResponse(saved);
        }
    }

    @Transactional
    public void unlikeContent(LikeRequest request) {
        Profile user = profileRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate exactly one content ID is provided
        int contentCount = 0;
        if (request.getPostId() != null) contentCount++;
        if (request.getReelId() != null) contentCount++;
        if (request.getCampaignId() != null) contentCount++;

        if (contentCount != 1) {
            throw new IllegalArgumentException(
                    "Exactly one of postId, reelId, or campaignId must be provided"
            );
        }

        if (request.getPostId() != null) {
            Like like = likeRepository.findByUserIdAndPostId(user.getId(), request.getPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Like not found"));
            likeRepository.delete(like);
        }
        else if (request.getReelId() != null) {
            Like like = likeRepository.findByUserIdAndReelId(user.getId(), request.getReelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Like not found"));
            likeRepository.delete(like);
        }
        else {
            Like like = likeRepository.findByUserIdAndCampaignId(
                    user.getId(), request.getCampaignId()
            ).orElseThrow(() -> new ResourceNotFoundException("Like not found"));
            likeRepository.delete(like);
        }
    }

    public long getPostLikesCount(String postId) {
        return likeRepository.countByPostId(postId);
    }

    public long getReelLikesCount(String reelId) {
        return likeRepository.countByReelId(reelId);
    }

    public long getCampaignLikesCount(Long campaignId) {
        return likeRepository.countByCampaignId(campaignId);
    }

    public boolean isPostLikedByUser(String postId, Long userId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    public boolean isReelLikedByUser(String reelId, Long userId) {
        return likeRepository.existsByUserIdAndReelId(userId, reelId);
    }

    public boolean isCampaignLikedByUser(Long campaignId, Long userId) {
        return likeRepository.existsByUserIdAndCampaignId(userId, campaignId);
    }

    private LikeResponse mapToLikeResponse(Like like) {
        LikeResponse response = new LikeResponse();
        response.setId(like.getId());
        response.setUserId(like.getUser().getId());
        response.setPostId(like.getPostId());
        response.setReelId(like.getReelId());
        response.setCampaignId(like.getCampaign() != null ? like.getCampaign().getId() : null);
        response.setContentType(like.getContentType().name());
        response.setCreatedAt(like.getCreatedAt().toString());
        return response;
    }
}