package com.instagram.backend.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawalResponse {
    private Long id;
    private Long campaignId;
    private String campaignTitle;
    private Long requesterId;
    private String requesterUsername;
    private BigDecimal amount;
    private String currency;
    private BigDecimal platformFee;
    private BigDecimal paymentGatewayFee;
    private BigDecimal netAmount;
    private Long bankAccountId;
    private String bankAccountMasked;
    private String status;
    private String transactionReference;
    private String reason;
    private String rejectionReason;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
