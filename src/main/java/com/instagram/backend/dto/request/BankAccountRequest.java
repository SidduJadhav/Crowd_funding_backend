package com.instagram.backend.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class BankAccountRequest {

    @NotNull(message = "Profile ID is required")
    private Long profileId;

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
    @Size(min = 8, max = 20, message = "Account number must be between 8 and 20 digits")
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    @Size(min = 2, max = 100, message = "Bank name must be between 2 and 100 characters")
    private String bankName;

    // Conditional validation based on country
    @Size(max = 11, message = "IFSC code must be maximum 11 characters")
    private String ifscCode;

    @Size(max = 11, message = "SWIFT code must be maximum 11 characters")
    private String swiftCode;

    @Size(max = 9, message = "Routing number must be maximum 9 characters")
    private String routingNumber;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "^(SAVINGS|CHECKING|CURRENT|BUSINESS)$",
            message = "Account type must be SAVINGS, CHECKING, CURRENT, or BUSINESS")
    private String accountType;

    @NotBlank(message = "Branch name is required")
    @Size(min = 2, max = 100, message = "Branch name must be between 2 and 100 characters")
    private String branchName;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 3, message = "Country code must be 2-3 characters")
    private String country;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency;

    @NotNull(message = "Primary flag is required")
    private Boolean isPrimary;
}