package com.instagram.backend.service;
import com.instagram.backend.dto.request.ProfileRequest;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.exception.AlreadyExistsException;


import com.instagram.backend.dto.request.ProfileUpdateRequest;
import com.instagram.backend.dto.response.ProfileResponse;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.model.entity.User;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private  final UserRepository userRepository;


    private final FollowService followService;

    public ProfileResponse getProfile(Long userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + userId));

        int followersCount = followService.getFollowersCount(userId);
        int followingCount = followService.getFollowingCount(userId);

        return mapToProfileResponse(profile, followersCount, followingCount);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest updateRequest) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found with id: " + userId));

        if (updateRequest.getName() != null) {
            profile.setName(updateRequest.getName());
        }
        if (updateRequest.getBio() != null) {
            profile.setBio(updateRequest.getBio());
        }
        if (updateRequest.getProfilePictureUrl() != null) {
            profile.setProfilePictureUrl(updateRequest.getProfilePictureUrl());
        }

        Profile updatedProfile = profileRepository.save(profile);
        int followersCount = followService.getFollowersCount(userId);
        int followingCount = followService.getFollowingCount(userId);

        return mapToProfileResponse(updatedProfile, followersCount, followingCount);
    }

    private ProfileResponse mapToProfileResponse(Profile profile, int followersCount, int followingCount) {
        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setUsername(profile.getUser().getUsername());
        response.setName(profile.getName());
        response.setBio(profile.getBio());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());
        response.setCreatedAt(profile.getCreatedAt());
        response.setFollowersCount(followersCount);
        response.setFollowingCount(followingCount);
        return response;
    }
    @Transactional

    public ProfileResponse createProfile(ProfileRequest profileRequest, String usernameFromToken) {
        // 1️⃣ Find the user who is creating the profile
        User user = userRepository.findByUsername(usernameFromToken)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameFromToken));

        // 2️⃣ Check if this user already has a profile


        // 3️⃣ Map request data to profile
        Profile profile = user.getProfile();

        profile.setUser(user);
        // or user.getName() if available
        profile.setName(profileRequest.getName());
        profile.setBio(profileRequest.getBio());
        profile.setProfilePictureUrl(profileRequest.getProfilePictureUrl());
        profile.setIsPrivate(profileRequest.getIsPrivate());

        // 4️⃣ Save the profile
        Profile savedProfile = profileRepository.save(profile);

        // 5️⃣ Map response
        return ProfileResponse.builder()
                .id(savedProfile.getId())
                .username(user.getUsername())
                .name(savedProfile.getName())
                .bio(savedProfile.getBio())
                .profilePictureUrl(savedProfile.getProfilePictureUrl())
                .createdAt(savedProfile.getCreatedAt())
                .followersCount(0)
                .followingCount(0)
                .build();
    }

}

