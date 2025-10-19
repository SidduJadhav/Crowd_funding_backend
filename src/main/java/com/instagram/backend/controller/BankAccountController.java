package com.instagram.backend.controller;

import com.instagram.backend.dto.request.BankAccountRequest;
import com.instagram.backend.dto.response.BankAccountResponse;
import com.instagram.backend.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<BankAccountResponse> addBankAccount(@Valid @RequestBody BankAccountRequest request) {
        BankAccountResponse account = bankAccountService.addBankAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccountResponse> getBankAccount(
            @PathVariable Long accountId,
            @RequestParam Long profileId) {
        BankAccountResponse account = bankAccountService.getBankAccountById(accountId, profileId);
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<BankAccountResponse> updateBankAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody BankAccountRequest request) {
        BankAccountResponse account = bankAccountService.updateBankAccount(accountId, request);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteBankAccount(
            @PathVariable Long accountId,
            @RequestParam Long profileId) {
        bankAccountService.deleteBankAccount(accountId, profileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{profileId}")
    public ResponseEntity<List<BankAccountResponse>> getUserBankAccounts(@PathVariable Long profileId) {
        List<BankAccountResponse> accounts = bankAccountService.getUserBankAccounts(profileId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/user/{profileId}/primary")
    public ResponseEntity<BankAccountResponse> getPrimaryBankAccount(@PathVariable Long profileId) {
        BankAccountResponse account = bankAccountService.getPrimaryBankAccount(profileId);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{accountId}/verify")
    public ResponseEntity<BankAccountResponse> verifyBankAccount(
            @PathVariable Long accountId,
            @RequestParam Long adminId) {
        BankAccountResponse account = bankAccountService.verifyBankAccount(accountId, adminId);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{accountId}/verification-document")
    public ResponseEntity<Void> uploadVerificationDocument(
            @PathVariable Long accountId,
            @RequestParam Long profileId,
            @RequestParam String documentUrl) {
        bankAccountService.uploadVerificationDocument(accountId, profileId, documentUrl);
        return ResponseEntity.ok().build();
    }
}