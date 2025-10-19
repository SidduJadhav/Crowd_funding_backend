package com.instagram.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DonationRequest {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum donation amount is 1")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotNull(message = "Anonymous flag is required")
    private Boolean isAnonymous = false;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    private Map<String, String> paymentDetails;//Generic payment details object
}