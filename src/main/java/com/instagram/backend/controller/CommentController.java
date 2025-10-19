package com.instagram.backend.controller;

import com.instagram.backend.dto.request.CommentRequest;
import com.instagram.backend.dto.response.CommentResponse;
import com.instagram.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request) {
        CommentResponse comment = commentService.createComment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse comment = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentResponse>> getPostComments(
            @PathVariable String postId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CommentResponse> comments = commentService.getPostComments(postId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/reel/{reelId}")
    public ResponseEntity<Page<CommentResponse>> getReelComments(
            @PathVariable String reelId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CommentResponse> comments = commentService.getReelComments(reelId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<Page<CommentResponse>> getCampaignComments(
            @PathVariable Long campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CommentResponse> comments = commentService.getCampaignComments(campaignId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{parentCommentId}/replies")
    public ResponseEntity<Page<CommentResponse>> getCommentReplies(
            @PathVariable String parentCommentId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CommentResponse> replies = commentService.getCommentReplies(parentCommentId, pageable);
        return ResponseEntity.ok(replies);
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> likeComment(@PathVariable String commentId) {
        commentService.incrementLikeCount(commentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(@PathVariable String commentId) {
        commentService.decrementLikeCount(commentId);
        return ResponseEntity.ok().build();
    }
}
