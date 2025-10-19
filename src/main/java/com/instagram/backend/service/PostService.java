package com.instagram.backend.service;

import com.instagram.backend.dto.request.PostRequest;
import com.instagram.backend.dto.response.PostResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.document.Post;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    @Autowired
    private final PostRepository postRepository;
    private final ProfileRepository profileRepository;
    // REMOVED: LikeService and CommentService dependencies to break circular dependency

    public PostResponse createPost(PostRequest postRequest) {
        Profile profile = profileRepository.findById(postRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        Post post = new Post();
        post.setUserId(postRequest.getUserId());
        post.setUsername(profile.getUser().getUsername());
        post.setProfilePictureUrl(profile.getProfilePictureUrl());
        post.setCaption(postRequest.getCaption());
        post.setMediaUrls(postRequest.getMediaUrls());
        post.setTags(postRequest.getTags());
        post.setLocation(postRequest.getLocation());
        post.setIsPublic(postRequest.getIsPublic() != null ? postRequest.getIsPublic() : true);

        Post savedPost = postRepository.save(post);
        return mapToPostResponse(savedPost, postRequest.getUserId());
    }

    public PostResponse updatePost(String postId, PostRequest postRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(postRequest.getUserId())) {
            throw new IllegalArgumentException("You can only update your own posts");
        }

        post.setCaption(postRequest.getCaption());
        post.setMediaUrls(postRequest.getMediaUrls());
        post.setTags(postRequest.getTags());
        post.setLocation(postRequest.getLocation());
        post.setUpdatedAt(LocalDateTime.now());

        Post updatedPost = postRepository.save(post);
        return mapToPostResponse(updatedPost, postRequest.getUserId());
    }

    public void deletePost(String postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own posts");
        }

        postRepository.delete(post);
    }

    public PostResponse getPostById(String postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return mapToPostResponse(post, userId);
    }

    public Long getPostOwnerId(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return post.getUserId();
    }

    public Page<PostResponse> getUserPosts(Long userId, Long currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return posts.map(post -> mapToPostResponse(post, currentUserId));
    }

    public Page<PostResponse> getFeedPosts(List<Long> followingIds, Long currentUserId, Pageable pageable) {
        if (followingIds == null || followingIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Post> posts = postRepository.findByUserIdInOrderByCreatedAtDesc(followingIds, pageable);
        return posts.map(post -> mapToPostResponse(post, currentUserId));
    }

    // Increment like count
    public void incrementLikeCount(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
    }

    // Decrement like count
    public void decrementLikeCount(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        postRepository.save(post);
    }

    // Increment comment count
    public void incrementCommentCount(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);
    }

    // Decrement comment count
    public void decrementCommentCount(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
    }

    private PostResponse mapToPostResponse(Post post, Long currentUserId) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setUsername(post.getUsername());
        response.setProfilePictureUrl(post.getProfilePictureUrl());
        response.setCaption(post.getCaption());
        response.setMediaUrls(post.getMediaUrls());
        response.setTags(post.getTags());
        response.setLocation(post.getLocation());
        response.setIsPublic(post.getIsPublic());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());

        // Use post's own counts instead of service calls
        response.setLikesCount(post.getLikesCount());
        response.setCommentsCount(post.getCommentsCount());

        return response;
    }
}