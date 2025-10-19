package com.instagram.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BankAccountResponse {
    private Long id;
    private Long profileId;
    private String accountHolderName;
    private String accountNumberMasked;
    private String bankName;
    private String ifscCode;
    private String swiftCode;
    private String routingNumber;
    private String accountType;
    private String branchName;
    private String country;
    private String currency;
    private Boolean isPrimary;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}