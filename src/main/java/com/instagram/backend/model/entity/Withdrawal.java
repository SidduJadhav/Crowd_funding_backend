package com.instagram.backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "withdrawals", indexes = {
        @Index(name = "idx_withdrawal_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_withdrawal_status", columnList = "status"),
        @Index(name = "idx_withdrawal_created_at", columnList = "created_at")
})
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Profile requester;

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "platform_fee", precision = 15, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "payment_gateway_fee", precision = 15, scale = 2)
    private BigDecimal paymentGatewayFee;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateFees();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private void calculateFees() {
        if (amount != null) {
            // Example: 5% platform fee
            platformFee = amount.multiply(new BigDecimal("0.05"));
            // Example: 2% payment gateway fee
            paymentGatewayFee = amount.multiply(new BigDecimal("0.02"));
            netAmount = amount.subtract(platformFee).subtract(paymentGatewayFee);
        }
    }

    public enum WithdrawalStatus {
        PENDING,
        UNDER_REVIEW,
        APPROVED,
        PROCESSING,
        COMPLETED,
        REJECTED,
        FAILED
    }
}