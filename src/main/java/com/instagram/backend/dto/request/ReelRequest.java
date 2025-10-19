package com.instagram.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class ReelRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Caption is required")
    @Size(max = 2200, message = "Caption must not exceed 2200 characters")
    private String caption;

    @NotBlank(message = "Video URL is required")
    private String videoUrl;

    private String thumbnailUrl;

    @NotNull(message = "Duration is required")
    private Integer duration; // Duration in seconds

    private List<String> tags;

    private String aspectRatio = "9:16"; // Default aspect ratio for reels

    private Boolean isPublic = true;
}