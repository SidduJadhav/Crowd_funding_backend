package com.instagram.backend.service;

import com.instagram.backend.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {
    SearchResponse searchAll(String query, Pageable pageable);
    Page<ProfileResponse> searchProfiles(String query, Pageable pageable);
    Page<PostResponse> searchPosts(String query, Pageable pageable);
    Page<ReelResponse> searchReels(String query, Pageable pageable);
}