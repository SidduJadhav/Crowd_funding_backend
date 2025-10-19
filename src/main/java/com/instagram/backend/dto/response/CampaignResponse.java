package com.instagram.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CampaignResponse {
    private Long id;
    private Long creatorId;
    private String creatorUsername;
    private String creatorName;
    private String creatorProfilePicture;
    private String title;
    private String description;
    private BigDecimal goalAmount;
    private BigDecimal currentAmount;
    private String currency;
    private String category;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String imageUrl;
    private String videoUrl;
    private Boolean isVerified;
    private String beneficiaryName;
    private Integer donorCount;
    private Long likesCount;
    private Long commentsCount;
    private Boolean liked;
    private BigDecimal progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
