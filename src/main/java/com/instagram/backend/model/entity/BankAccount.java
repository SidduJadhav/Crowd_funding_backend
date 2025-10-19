package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bank_accounts", indexes = {
        @Index(name = "idx_bank_account_profile_id", columnList = "profile_id"),
        @Index(name = "idx_bank_account_is_verified", columnList = "is_verified")
})
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @NotBlank(message = "Account holder name is required")
    @Column(name = "account_holder_name", nullable = false, length = 100)
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", nullable = false, length = 255)
    private String accountNumber; // Should be encrypted

    @NotBlank(message = "Bank name is required")
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode; // For India

    @Column(name = "swift_code", length = 20)
    private String swiftCode; // For international

    @Column(name = "routing_number", length = 20)
    private String routingNumber; // For US

    @Column(name = "account_type", length = 20)
    private String accountType; // SAVINGS, CURRENT, CHECKING

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @NotBlank(message = "Country is required")
    @Column(nullable = false, length = 50)
    private String country;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "verification_document_url")
    private String verificationDocumentUrl;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by_id")
    private Long verifiedById;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Mask account number for display
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }
}