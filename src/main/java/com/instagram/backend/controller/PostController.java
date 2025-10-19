package com.instagram.backend.controller;

import com.instagram.backend.dto.request.PostRequest;
import com.instagram.backend.dto.response.PostResponse;
import com.instagram.backend.service.PostService;
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
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        PostResponse post = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String postId,
            @RequestParam(required = false) Long userId) {
        PostResponse post = postService.getPostById(postId, userId);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable String postId,
            @Valid @RequestBody PostRequest request) {
        PostResponse post = postService.updatePost(postId, request);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String postId,
            @RequestParam Long userId) {
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(required = false) Long currentUserId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> posts = postService.getUserPosts(userId, currentUserId, pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PostResponse>> getFeedPosts(
            @RequestParam Long userId,
            @RequestParam List<Long> followingIds,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> posts = postService.getFeedPosts(followingIds, userId, pageable);
        return ResponseEntity.ok(posts);
    }
}
