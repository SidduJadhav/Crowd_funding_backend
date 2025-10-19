package com.instagram.backend.controller;

import com.instagram.backend.dto.response.*;
import com.instagram.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> searchAll(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        SearchResponse results = searchService.searchAll(query, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/profiles")
    public ResponseEntity<Page<ProfileResponse>> searchProfiles(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProfileResponse> profiles = searchService.searchProfiles(query, pageable);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/posts")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PostResponse> posts = searchService.searchPosts(query, pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/reels")
    public ResponseEntity<Page<ReelResponse>> searchReels(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReelResponse> reels = searchService.searchReels(query, pageable);
        return ResponseEntity.ok(reels);
    }
}
