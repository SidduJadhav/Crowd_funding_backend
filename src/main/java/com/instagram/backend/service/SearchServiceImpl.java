package com.instagram.backend.service;

import com.instagram.backend.dto.response.*;
import com.instagram.backend.model.document.Post;
import com.instagram.backend.model.document.Reel;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.mongo.PostRepository;
import com.instagram.backend.repository.mongo.ReelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;

    @Override
    public SearchResponse searchAll(String query, Pageable pageable) {
        SearchResponse response = new SearchResponse();
        response.setProfiles(searchProfiles(query, pageable));
        response.setPosts(searchPosts(query, pageable));
        response.setReels(searchReels(query, pageable));
        return response;
    }

    @Override
    public Page<ProfileResponse> searchProfiles(String query, Pageable pageable) {
        return profileRepository.findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(query, query, pageable)
                .map(this::mapToProfileResponse);
    }

    @Override
    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
        return postRepository.findByCaptionContainingIgnoreCaseOrTagsIn(query, List.of(query), pageable)
                .map(this::mapToPostResponse);
    }

    @Override
    public Page<ReelResponse> searchReels(String query, Pageable pageable) {
        return reelRepository.findByCaptionContainingIgnoreCaseOrTagsIn(query, List.of(query), pageable)
                .map(this::mapToReelResponse);
    }

    private ProfileResponse mapToProfileResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setUsername(profile.getUser().getUsername());
        response.setName(profile.getName());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());
        return response;
    }

    private PostResponse mapToPostResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setCaption(post.getCaption());
        response.setMediaUrls(post.getMediaUrls());
        response.setTags(post.getTags());
        return response;
    }

    private ReelResponse mapToReelResponse(Reel reel) {
        ReelResponse response = new ReelResponse();
        response.setId(reel.getId());
        response.setCaption(reel.getCaption());
        response.setVideoUrl(reel.getVideoUrl());
        response.setTags(reel.getTags());
        return response;
    }
}