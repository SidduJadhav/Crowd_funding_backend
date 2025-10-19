package com.instagram.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    private String profilePictureUrl;
}