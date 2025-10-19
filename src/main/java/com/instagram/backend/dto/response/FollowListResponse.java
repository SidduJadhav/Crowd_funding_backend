package com.instagram.backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class FollowListResponse {
    private List<FollowResponse> users;
    private int totalCount;
}