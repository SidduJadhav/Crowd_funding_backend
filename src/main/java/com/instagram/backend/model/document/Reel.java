package com.instagram.backend.model.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "reels")
public class Reel {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String username;
    private String profilePictureUrl; // ADDED: Missing field
    private String caption;
    private String videoUrl;
    private String thumbnailUrl; // ADDED: Missing field
    private Integer duration; // ADDED: Missing field (in seconds)
    private String aspectRatio = "9:16"; // ADDED: Missing field

    private List<String> tags = new ArrayList<>();

    // Count fields for performance
    private Integer viewCount = 0;
    private Integer likesCount = 0;
    private Integer commentsCount = 0;
    private Integer sharesCount = 0; // ADDED: Missing field

    private Boolean isPublic = true; // ADDED: Missing field

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // ADDED: Indexed fields for better query performance
    @Indexed
    private LocalDateTime indexedCreatedAt = LocalDateTime.now();
}