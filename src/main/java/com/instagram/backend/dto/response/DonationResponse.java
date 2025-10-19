package com.instagram.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DonationResponse {
    private Long id;
    private Long campaignId;
    private String campaignTitle;
    private Long donorId;
    private String donorUsername;
    private String donorName;
    private String donorProfilePicture;
    private BigDecimal amount;
    private String currency;
    private String message;
    private String paymentStatus;
    private String paymentMethod;
    private String transactionId;
    private String refundId;           // ADD THIS
    private BigDecimal refundAmount;   // ADD THIS

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;   //
}