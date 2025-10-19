package com.instagram.backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private Long id;
    private String username;
    private String name;
    private String bio;
    private String profilePictureUrl;
    private LocalDateTime createdAt;
    private int followersCount;
    private int followingCount;

    public boolean hasProfilePicture() {
        return profilePictureUrl != null && !profilePictureUrl.isBlank();
    }

    public boolean isNewAccount() {
        return createdAt != null && createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    public boolean isPopular() {
        return followersCount > 1000;
    }
}