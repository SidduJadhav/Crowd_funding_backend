package com.instagram.backend.controller;

import com.instagram.backend.dto.request.WithdrawalRequest;
import com.instagram.backend.dto.response.WithdrawalResponse;
import com.instagram.backend.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping
    public ResponseEntity<WithdrawalResponse> requestWithdrawal(@Valid @RequestBody WithdrawalRequest request) {
        WithdrawalResponse withdrawal = withdrawalService.requestWithdrawal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(withdrawal);
    }

    @GetMapping("/{withdrawalId}")
    public ResponseEntity<WithdrawalResponse> getWithdrawal(@PathVariable Long withdrawalId) {
        WithdrawalResponse withdrawal = withdrawalService.getWithdrawalById(withdrawalId);
        return ResponseEntity.ok(withdrawal);
    }

    @PostMapping("/{withdrawalId}/approve")
    public ResponseEntity<WithdrawalResponse> approveWithdrawal(
            @PathVariable Long withdrawalId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String notes) {
        WithdrawalResponse withdrawal = withdrawalService.approveWithdrawal(withdrawalId, adminId, notes);
        return ResponseEntity.ok(withdrawal);
    }

    @PostMapping("/{withdrawalId}/reject")
    public ResponseEntity<WithdrawalResponse> rejectWithdrawal(
            @PathVariable Long withdrawalId,
            @RequestParam Long adminId,
            @RequestParam String reason) {
        WithdrawalResponse withdrawal = withdrawalService.rejectWithdrawal(withdrawalId, adminId, reason);
        return ResponseEntity.ok(withdrawal);
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<Page<WithdrawalResponse>> getCampaignWithdrawals(
            @PathVariable Long campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WithdrawalResponse> withdrawals = withdrawalService.getCampaignWithdrawals(campaignId, pageable);
        return ResponseEntity.ok(withdrawals);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<WithdrawalResponse>> getUserWithdrawals(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WithdrawalResponse> withdrawals = withdrawalService.getUserWithdrawals(userId, pageable);
        return ResponseEntity.ok(withdrawals);
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<WithdrawalResponse>> getPendingWithdrawals(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WithdrawalResponse> withdrawals = withdrawalService.getPendingWithdrawals(pageable);
        return ResponseEntity.ok(withdrawals);
    }

    @GetMapping("/campaign/{campaignId}/total")
    public ResponseEntity<BigDecimal> getTotalWithdrawn(@PathVariable Long campaignId) {
        BigDecimal total = withdrawalService.getTotalWithdrawnAmount(campaignId);
        return ResponseEntity.ok(total);
    }
}