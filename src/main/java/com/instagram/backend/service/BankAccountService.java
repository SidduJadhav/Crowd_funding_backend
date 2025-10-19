package com.instagram.backend.service;

import com.instagram.backend.dto.request.BankAccountRequest;
import com.instagram.backend.dto.response.BankAccountResponse;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.BankAccount;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.repository.jpa.BankAccountRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    // In production, use proper key management (AWS KMS, HashiCorp Vault, etc.)
    private static final String ENCRYPTION_KEY = "MySecretKey12345"; // 16 chars for AES-128

    @Transactional
    public BankAccountResponse addBankAccount(BankAccountRequest request) {
        Profile profile = profileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        // Validate account number format
        validateAccountNumber(request.getAccountNumber(), request.getCountry());

        BankAccount bankAccount = new BankAccount();
        bankAccount.setProfile(profile);
        bankAccount.setAccountHolderName(request.getAccountHolderName());

        // Encrypt account number before storing
        bankAccount.setAccountNumber(encryptAccountNumber(request.getAccountNumber()));

        bankAccount.setBankName(request.getBankName());
        bankAccount.setIfscCode(request.getIfscCode());
        bankAccount.setSwiftCode(request.getSwiftCode());
        bankAccount.setRoutingNumber(request.getRoutingNumber());
        bankAccount.setAccountType(request.getAccountType());
        bankAccount.setBranchName(request.getBranchName());
        bankAccount.setCountry(request.getCountry());
        bankAccount.setCurrency(request.getCurrency());
        bankAccount.setIsPrimary(request.getIsPrimary());

        // If this is set as primary, unset other primary accounts
        if (request.getIsPrimary()) {
            setPrimaryAccount(profile.getId(), null);
        }

        BankAccount saved = bankAccountRepository.save(bankAccount);
        return mapToBankAccountResponse(saved);
    }

    @Transactional
    public BankAccountResponse updateBankAccount(Long accountId, BankAccountRequest request) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!bankAccount.getProfile().getId().equals(request.getProfileId())) {
            throw new IllegalArgumentException("Bank account does not belong to this profile");
        }

        if (bankAccount.getIsVerified()) {
            throw new IllegalArgumentException("Cannot update verified bank account");
        }

        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setBranchName(request.getBranchName());

        if (request.getIsPrimary() && !bankAccount.getIsPrimary()) {
            setPrimaryAccount(request.getProfileId(), accountId);
            bankAccount.setIsPrimary(true);
        }

        BankAccount updated = bankAccountRepository.save(bankAccount);
        return mapToBankAccountResponse(updated);
    }

    @Transactional
    public void deleteBankAccount(Long accountId, Long profileId) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!bankAccount.getProfile().getId().equals(profileId)) {
            throw new IllegalArgumentException("Bank account does not belong to this profile");
        }

        if (bankAccount.getIsVerified()) {
            // Soft delete - just deactivate
            bankAccount.setIsActive(false);
            bankAccountRepository.save(bankAccount);
        } else {
            // Hard delete if not verified
            bankAccountRepository.delete(bankAccount);
        }
    }

    @Transactional
    public BankAccountResponse verifyBankAccount(Long accountId, Long adminId) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (bankAccount.getIsVerified()) {
            throw new IllegalArgumentException("Bank account is already verified");
        }

        if (bankAccount.getVerificationDocumentUrl() == null) {
            throw new IllegalArgumentException("Verification document is required");
        }

        bankAccount.setIsVerified(true);
        bankAccount.setVerifiedAt(LocalDateTime.now());
        bankAccount.setVerifiedById(adminId);

        BankAccount verified = bankAccountRepository.save(bankAccount);

        // Notify user
        notificationService.createBankAccountVerifiedNotification(
                bankAccount.getProfile().getId(),
                accountId
        );

        return mapToBankAccountResponse(verified);
    }

    @Transactional
    public void uploadVerificationDocument(Long accountId, Long profileId, String documentUrl) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!bankAccount.getProfile().getId().equals(profileId)) {
            throw new IllegalArgumentException("Bank account does not belong to this profile");
        }

        bankAccount.setVerificationDocumentUrl(documentUrl);
        bankAccountRepository.save(bankAccount);

        // Notify admins for verification
        notificationService.createBankAccountVerificationRequestNotification(accountId);
    }

    public BankAccountResponse getBankAccountById(Long accountId, Long profileId) {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!bankAccount.getProfile().getId().equals(profileId)) {
            throw new IllegalArgumentException("Unauthorized access");
        }

        return mapToBankAccountResponse(bankAccount);
    }

    public List<BankAccountResponse> getUserBankAccounts(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        List<BankAccount> accounts = bankAccountRepository.findByProfileIdAndIsActiveTrue(profileId);
        return accounts.stream()
                .map(this::mapToBankAccountResponse)
                .collect(Collectors.toList());
    }

    public BankAccountResponse getPrimaryBankAccount(Long profileId) {
        BankAccount primary = bankAccountRepository
                .findByProfileIdAndIsPrimaryTrueAndIsActiveTrue(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("No primary bank account found"));

        return mapToBankAccountResponse(primary);
    }

    public List<BankAccountResponse> getUnverifiedAccounts() {
        List<BankAccount> unverified = bankAccountRepository
                .findByIsVerifiedFalseAndVerificationDocumentUrlIsNotNull();
        return unverified.stream()
                .map(this::mapToBankAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    protected void setPrimaryAccount(Long profileId, Long newPrimaryId) {
        List<BankAccount> accounts = bankAccountRepository.findByProfileId(profileId);
        for (BankAccount account : accounts) {
            if (account.getId().equals(newPrimaryId)) {
                account.setIsPrimary(true);
            } else {
                account.setIsPrimary(false);
            }
        }
        bankAccountRepository.saveAll(accounts);
    }

    private void validateAccountNumber(String accountNumber, String country) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        // Basic validation - expand based on country requirements
        if (country.equalsIgnoreCase("IN") && accountNumber.length() < 9) {
            throw new IllegalArgumentException("Invalid Indian account number format");
        }

        if (country.equalsIgnoreCase("US") && accountNumber.length() < 8) {
            throw new IllegalArgumentException("Invalid US account number format");
        }
    }

    private String encryptAccountNumber(String accountNumber) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(accountNumber.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decryptAccountNumber(String encryptedAccountNumber) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedAccountNumber));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private BankAccountResponse mapToBankAccountResponse(BankAccount account) {
        BankAccountResponse response = new BankAccountResponse();
        response.setId(account.getId());
        response.setProfileId(account.getProfile().getId());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setAccountNumberMasked(account.getMaskedAccountNumber());
        response.setBankName(account.getBankName());
        response.setIfscCode(account.getIfscCode());
        response.setSwiftCode(account.getSwiftCode());
        response.setRoutingNumber(account.getRoutingNumber());
        response.setAccountType(account.getAccountType());
        response.setBranchName(account.getBranchName());
        response.setCountry(account.getCountry());
        response.setCurrency(account.getCurrency());
        response.setIsPrimary(account.getIsPrimary());
        response.setIsVerified(account.getIsVerified());
        response.setIsActive(account.getIsActive());
        response.setVerifiedAt(account.getVerifiedAt());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }
}