package com.instagram.backend.dto.response;

import lombok.Data;

@Data
public class FollowResponse {
    private Long id;
    private String username;
    private String name;
    private String profilePictureUrl;
    private Boolean isPrivate;
}