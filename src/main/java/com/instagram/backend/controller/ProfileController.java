package com.instagram.backend.controller;

import com.instagram.backend.dto.request.ProfileRequest;
import com.instagram.backend.dto.request.ProfileUpdateRequest;
import com.instagram.backend.dto.response.ProfileResponse;
import com.instagram.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody ProfileRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ProfileResponse profile = profileService.createProfile(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }


    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long userId) {
        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileResponse profile = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(profile);
    }
}
