package com.instagram.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {



    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    @Size(max = 150, message = "Bio cannot exceed 150 characters")
    private String bio;

    private String profilePictureUrl;
    private Boolean isPrivate = false;

    public boolean hasProfilePicture() {
        return profilePictureUrl != null && !profilePictureUrl.isBlank();
    }

    public boolean isPrivateAccount() {
        return isPrivate != null && isPrivate;
    }
}