package com.instagram.backend.service;

import com.instagram.backend.dto.request.WithdrawalRequest;
import com.instagram.backend.dto.response.WithdrawalResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.BankAccount;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.model.entity.Withdrawal;
import com.instagram.backend.repository.jpa.BankAccountRepository;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.jpa.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final CampaignRepository campaignRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ProfileRepository profileRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Transactional
    public WithdrawalResponse requestWithdrawal(WithdrawalRequest request) {
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        Profile requester = profileRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Validate requester is campaign creator
        if (!campaign.getCreator().getId().equals(request.getRequesterId())) {
            throw new IllegalArgumentException("Only campaign creator can request withdrawal");
        }

        // Validate bank account
        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!bankAccount.getProfile().getId().equals(request.getRequesterId())) {
            throw new IllegalArgumentException("Bank account does not belong to requester");
        }

        if (!bankAccount.getIsVerified()) {
            throw new IllegalArgumentException("Bank account must be verified");
        }

        if (!bankAccount.getIsActive()) {
            throw new IllegalArgumentException("Bank account is not active");
        }

        // Validate withdrawal amount
        if (request.getAmount().compareTo(campaign.getCurrentAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount exceeds available campaign funds"
            );
        }

        if (request.getAmount().compareTo(new BigDecimal("1")) < 0) {
            throw new IllegalArgumentException("Minimum withdrawal amount is 1");
        }

        // Create withdrawal request
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setCampaign(campaign);
        withdrawal.setRequester(requester);
        withdrawal.setAmount(request.getAmount());
        withdrawal.setCurrency(request.getCurrency());
        withdrawal.setBankAccount(bankAccount);
        withdrawal.setReason(request.getReason());
        withdrawal.setStatus(Withdrawal.WithdrawalStatus.PENDING);

        Withdrawal saved = withdrawalRepository.save(withdrawal);

        // Notify admins for approval
        notificationService.createWithdrawalRequestNotification(saved.getId());

        return mapToWithdrawalResponse(saved);
    }

    @Transactional
    public WithdrawalResponse approveWithdrawal(Long withdrawalId, Long adminId, String notes) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));

        if (withdrawal.getStatus() != Withdrawal.WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException("Only pending withdrawals can be approved");
        }

        withdrawal.setStatus(Withdrawal.WithdrawalStatus.APPROVED);
        withdrawal.setApprovedById(adminId);
        withdrawal.setApprovedAt(LocalDateTime.now());
        withdrawal.setAdminNotes(notes);

        Withdrawal approved = withdrawalRepository.save(withdrawal);

        // Process payment transfer
        processWithdrawalTransfer(approved);

        // Notify creator
        notificationService.createWithdrawalApprovedNotification(
                withdrawal.getRequester().getId(),
                withdrawalId
        );

        return mapToWithdrawalResponse(approved);
    }

    @Transactional
    public WithdrawalResponse rejectWithdrawal(
            Long withdrawalId, Long adminId, String reason
    ) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));

        if (withdrawal.getStatus() != Withdrawal.WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException("Only pending withdrawals can be rejected");
        }

        withdrawal.setStatus(Withdrawal.WithdrawalStatus.REJECTED);
        withdrawal.setRejectionReason(reason);
        withdrawal.setAdminNotes(reason);

        Withdrawal rejected = withdrawalRepository.save(withdrawal);

        // Notify creator
        notificationService.createWithdrawalRejectedNotification(
                withdrawal.getRequester().getId(),
                withdrawalId,
                reason
        );

        return mapToWithdrawalResponse(rejected);
    }

    @Transactional
    protected void processWithdrawalTransfer(Withdrawal withdrawal) {
        try {
            withdrawal.setStatus(Withdrawal.WithdrawalStatus.PROCESSING);
            withdrawalRepository.save(withdrawal);

            // Process bank transfer via payment gateway
            String transactionRef = paymentService.processBankTransfer(
                    withdrawal.getNetAmount(),
                    withdrawal.getCurrency(),
                    withdrawal.getBankAccount()
            );

            withdrawal.setTransactionReference(transactionRef);
            withdrawal.setStatus(Withdrawal.WithdrawalStatus.COMPLETED);
            withdrawal.setProcessedAt(LocalDateTime.now());
            withdrawalRepository.save(withdrawal);

            // Update campaign balance
            Campaign campaign = withdrawal.getCampaign();
            campaign.setCurrentAmount(
                    campaign.getCurrentAmount().subtract(withdrawal.getAmount())
            );
            campaignRepository.save(campaign);

            // Notify completion
            notificationService.createWithdrawalCompletedNotification(
                    withdrawal.getRequester().getId(),
                    withdrawal.getId()
            );

        } catch (Exception e) {
            withdrawal.setStatus(Withdrawal.WithdrawalStatus.FAILED);
            withdrawal.setRejectionReason("Transfer failed: " + e.getMessage());
            withdrawalRepository.save(withdrawal);

            notificationService.createWithdrawalFailedNotification(
                    withdrawal.getRequester().getId(),
                    withdrawal.getId(),
                    e.getMessage()
            );

            throw new RuntimeException("Withdrawal transfer failed: " + e.getMessage());
        }
    }

    public WithdrawalResponse getWithdrawalById(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));
        return mapToWithdrawalResponse(withdrawal);
    }

    public Page<WithdrawalResponse> getCampaignWithdrawals(
            Long campaignId, Pageable pageable
    ) {
        Page<Withdrawal> withdrawals = withdrawalRepository
                .findByCampaignId(campaignId, pageable);
        return withdrawals.map(this::mapToWithdrawalResponse);
    }

    public Page<WithdrawalResponse> getUserWithdrawals(Long userId, Pageable pageable) {
        Page<Withdrawal> withdrawals = withdrawalRepository
                .findByRequesterId(userId, pageable);
        return withdrawals.map(this::mapToWithdrawalResponse);
    }

    public Page<WithdrawalResponse> getPendingWithdrawals(Pageable pageable) {
        Page<Withdrawal> withdrawals = withdrawalRepository
                .findByStatus(Withdrawal.WithdrawalStatus.PENDING, pageable);
        return withdrawals.map(this::mapToWithdrawalResponse);
    }

    public BigDecimal getTotalWithdrawnAmount(Long campaignId) {
        return withdrawalRepository.getTotalWithdrawnByCampaign(campaignId);
    }

    private WithdrawalResponse mapToWithdrawalResponse(Withdrawal withdrawal) {
        WithdrawalResponse response = new WithdrawalResponse();
        response.setId(withdrawal.getId());
        response.setCampaignId(withdrawal.getCampaign().getId());
        response.setCampaignTitle(withdrawal.getCampaign().getTitle());
        response.setRequesterId(withdrawal.getRequester().getId());
        response.setRequesterUsername(withdrawal.getRequester().getUser().getUsername());
        response.setAmount(withdrawal.getAmount());
        response.setCurrency(withdrawal.getCurrency());
        response.setPlatformFee(withdrawal.getPlatformFee());
        response.setPaymentGatewayFee(withdrawal.getPaymentGatewayFee());
        response.setNetAmount(withdrawal.getNetAmount());
        response.setBankAccountId(withdrawal.getBankAccount().getId());
        response.setBankAccountMasked(withdrawal.getBankAccount().getMaskedAccountNumber());
        response.setStatus(withdrawal.getStatus().name());
        response.setTransactionReference(withdrawal.getTransactionReference());
        response.setReason(withdrawal.getReason());
        response.setRejectionReason(withdrawal.getRejectionReason());
        response.setApprovedAt(withdrawal.getApprovedAt());
        response.setProcessedAt(withdrawal.getProcessedAt());
        response.setCreatedAt(withdrawal.getCreatedAt());
        return response;
    }
}