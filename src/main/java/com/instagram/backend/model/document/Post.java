package com.instagram.backend.model.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "posts")
public class Post {

    @Id
    private String id;
    private Long userId;
    private String username;
    private String caption;
    private String profilePictureUrl;
    private List<String> mediaUrls = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private String location; // ADDED: Missing field
    private Boolean isPublic = true; // ADDED: Missing field
    private Integer likesCount = 0;
    private Integer commentsCount = 0;
    private Integer sharesCount = 0;
}
