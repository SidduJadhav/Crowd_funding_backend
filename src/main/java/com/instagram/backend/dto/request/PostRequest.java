package com.instagram.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class PostRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Caption is required")
    @Size(max = 2200, message = "Caption must not exceed 2200 characters")
    private String caption;

    @NotNull(message = "Media URLs are required")
    @Size(min = 1, max = 10, message = "Post must contain 1-10 media items")
    private List<String> mediaUrls;

    private List<String> tags;

    private String location;

    private Boolean isPublic = true;
}