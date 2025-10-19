package com.instagram.backend.controller;

import com.instagram.backend.dto.request.ReportRequest;
import com.instagram.backend.dto.response.ReportResponse;
import com.instagram.backend.model.entity.Report;
import com.instagram.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody ReportRequest request) {
        ReportResponse report = reportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long reportId) {
        ReportResponse report = reportService.getReportById(reportId);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/review")
    public ResponseEntity<ReportResponse> reviewReport(
            @PathVariable Long reportId,
            @RequestParam Long adminId,
            @RequestParam String action,
            @RequestParam(required = false) String notes) {
        ReportResponse report = reportService.reviewReport(reportId, adminId, action, notes);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
            @PathVariable Long reportId,
            @RequestParam Long adminId,
            @RequestParam String action,
            @RequestParam(required = false) String notes) {
        ReportResponse report = reportService.resolveReport(reportId, adminId, action, notes);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/dismiss")
    public ResponseEntity<ReportResponse> dismissReport(
            @PathVariable Long reportId,
            @RequestParam Long adminId,
            @RequestParam String reason) {
        ReportResponse report = reportService.dismissReport(reportId, adminId, reason);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{reportId}/escalate")
    public ResponseEntity<ReportResponse> escalateReport(
            @PathVariable Long reportId,
            @RequestParam Long adminId,
            @RequestParam(required = false) String notes) {
        ReportResponse report = reportService.escalateReport(reportId, adminId, notes);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<ReportResponse>> getPendingReports(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportResponse> reports = reportService.getPendingReports(pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ReportResponse>> getReportsByStatus(
            @PathVariable Report.ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportResponse> reports = reportService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReportResponse>> getUserReports(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportResponse> reports = reportService.getUserReports(userId, pageable);
        return ResponseEntity.ok(reports);
    }
}