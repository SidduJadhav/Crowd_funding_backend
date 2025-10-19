package com.instagram.backend.service;

import com.instagram.backend.dto.request.CommentRequest;
import com.instagram.backend.dto.response.CommentResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.document.Comment;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;
    private final PostService postService;


    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        Profile profile = profileRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        Comment comment = new Comment();
        comment.setUserId(request.getUserId());
        comment.setUsername(profile.getUser().getUsername());
        comment.setProfilePictureUrl(profile.getProfilePictureUrl());
        comment.setContent(request.getContent());
        comment.setPostId(request.getPostId());
        comment.setReelId(request.getReelId());
        comment.setCampaignId(request.getCampaignId());
        comment.setParentCommentId(request.getParentCommentId());

        // Validate that exactly one content reference is set
        try {
            comment.validateContentReference();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        Comment savedComment = commentRepository.save(comment);
        postService.incrementCommentCount(request.getPostId());



        // Update reply count for parent comment if this is a reply
        if (request.getParentCommentId() != null) {
            incrementReplyCount(request.getParentCommentId());
        }

        // Send notification - FIXED: Handle null values properly
        notificationService.createCommentNotification(
                savedComment.getPostId(),
                savedComment.getReelId(),
                savedComment.getCampaignId(),
                savedComment.getUserId(),
                request.getParentCommentId() != null
        );

        return mapToCommentResponse(savedComment);
    }

    @Transactional
    public CommentResponse updateComment(String commentId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("You can only update your own comments");
        }

        // FIXED: Use Boolean.TRUE.equals for null-safe check
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new IllegalArgumentException("Cannot update deleted comment");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return mapToCommentResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(String commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        // Soft delete
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // Update reply count for parent if this was a reply
        if (comment.getParentCommentId() != null) {
            decrementReplyCount(comment.getParentCommentId());
        }
    }

    public Page<CommentResponse> getPostComments(String postId, Pageable pageable) {
        Page<Comment> comments = commentRepository
                .findByPostIdAndIsDeletedFalseOrderByCreatedAtDesc(postId, pageable);
        return comments.map(this::mapToCommentResponse);
    }

    public Page<CommentResponse> getReelComments(String reelId, Pageable pageable) {
        Page<Comment> comments = commentRepository
                .findByReelIdAndIsDeletedFalseOrderByCreatedAtDesc(reelId, pageable);
        return comments.map(this::mapToCommentResponse);
    }

    public Page<CommentResponse> getCampaignComments(Long campaignId, Pageable pageable) {
        Page<Comment> comments = commentRepository
                .findByCampaignIdAndIsDeletedFalseOrderByCreatedAtDesc(campaignId, pageable);
        return comments.map(this::mapToCommentResponse);
    }

    public Page<CommentResponse> getCommentReplies(String parentCommentId, Pageable pageable) {
        // FIXED: Check if parent comment exists and is not deleted
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));

        if (Boolean.TRUE.equals(parent.getIsDeleted())) {
            throw new IllegalArgumentException("Parent comment is deleted");
        }

        return commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(
                parentCommentId, pageable
        ).map(this::mapToCommentResponse);
    }

    public long getPostCommentsCount(String postId) {
        return commentRepository.countByPostIdAndIsDeletedFalse(postId);
    }

    public long getReelCommentsCount(String reelId) {
        return commentRepository.countByReelIdAndIsDeletedFalse(reelId);
    }

    public long getCampaignCommentsCount(Long campaignId) {
        return commentRepository.countByCampaignIdAndIsDeletedFalse(campaignId);
    }

    @Transactional
    public void incrementLikeCount(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // FIXED: Null-safe like count increment
        Integer currentLikes = comment.getLikeCount();
        comment.setLikeCount(currentLikes != null ? currentLikes + 1 : 1);
        commentRepository.save(comment);
    }

    @Transactional
    public void decrementLikeCount(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // FIXED: Null-safe like count decrement
        Integer currentLikes = comment.getLikeCount();
        comment.setLikeCount(Math.max(0, currentLikes != null ? currentLikes - 1 : 0));
        commentRepository.save(comment);
    }

    private void incrementReplyCount(String parentCommentId) {
        Comment parent = commentRepository.findById(parentCommentId).orElse(null);
        if (parent != null) {
            // FIXED: Null-safe reply count increment
            Integer currentReplies = parent.getReplyCount();
            parent.setReplyCount(currentReplies != null ? currentReplies + 1 : 1);
            commentRepository.save(parent);
        }
    }

    private void decrementReplyCount(String parentCommentId) {
        Comment parent = commentRepository.findById(parentCommentId).orElse(null);
        if (parent != null) {
            // FIXED: Null-safe reply count decrement
            Integer currentReplies = parent.getReplyCount();
            parent.setReplyCount(Math.max(0, currentReplies != null ? currentReplies - 1 : 0));
            commentRepository.save(parent);
        }
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setUserId(comment.getUserId());
        response.setUsername(comment.getUsername());
        response.setProfilePictureUrl(comment.getProfilePictureUrl());
        response.setContent(comment.getContent());
        response.setPostId(comment.getPostId());
        response.setReelId(comment.getReelId());
        response.setCampaignId(comment.getCampaignId());
        response.setParentCommentId(comment.getParentCommentId());
        response.setLikeCount(comment.getLikeCount());
        response.setReplyCount(comment.getReplyCount());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }
}