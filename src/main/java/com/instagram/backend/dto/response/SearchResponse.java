package com.instagram.backend.dto.response;

import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class SearchResponse {
    private Page<ProfileResponse> profiles;
    private Page<PostResponse> posts;
    private Page<ReelResponse> reels;
}