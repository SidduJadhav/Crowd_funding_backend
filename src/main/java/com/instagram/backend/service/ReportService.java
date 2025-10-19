package com.instagram.backend.service;

import com.instagram.backend.dto.request.ReportRequest;
import com.instagram.backend.dto.response.ReportResponse;
import com.instagram.backend.exception.AlreadyExistsException;
import com.instagram.backend.exception.ResourceNotFoundException;
import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.model.entity.Report;
import com.instagram.backend.repository.jpa.CampaignRepository;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.jpa.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProfileRepository profileRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReportResponse createReport(ReportRequest request) {
        Profile reportedBy = profileRepository.findById(request.getReportedByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Reporter profile not found"));

        // Validate content type and ID
        validateReportRequest(request);

        // Check for duplicate reports
        if (isDuplicateReport(request)) {
            throw new AlreadyExistsException("You have already reported this content");
        }

        Report report = new Report();
        report.setReportedBy(reportedBy);
        report.setContentType(request.getContentType());
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus(Report.ReportStatus.PENDING);

        // Set content references based on type
        switch (request.getContentType()) {
            case POST:
                report.setPostId(request.getPostId());
                break;
            case REEL:
                report.setReelId(request.getReelId());
                break;
            case COMMENT:
                report.setCommentId(request.getCommentId());
                break;
            case CAMPAIGN:
                Campaign campaign = campaignRepository.findById(request.getCampaignId())
                        .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
                report.setCampaign(campaign);
                break;
            case PROFILE:
                Profile reportedUser = profileRepository.findById(request.getReportedUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));
                report.setReportedUser(reportedUser);
                break;
        }

        Report savedReport = reportRepository.save(report);

        // Notify admins about new report
        notificationService.createNewReportNotification(savedReport.getId());

        return mapToReportResponse(savedReport);
    }

    @Transactional
    public ReportResponse reviewReport(Long reportId, Long adminId, String action, String notes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new IllegalArgumentException("Only pending reports can be reviewed");
        }

        report.setStatus(Report.ReportStatus.UNDER_REVIEW);
        report.setReviewedById(adminId);
        report.setReviewNotes(notes);
        report.setReviewedAt(LocalDateTime.now());

        Report reviewed = reportRepository.save(report);
        return mapToReportResponse(reviewed);
    }

    @Transactional
    public ReportResponse resolveReport(Long reportId, Long adminId, String action, String notes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() == Report.ReportStatus.RESOLVED) {
            throw new IllegalArgumentException("Report is already resolved");
        }

        report.setStatus(Report.ReportStatus.RESOLVED);
        report.setReviewedById(adminId);
        report.setReviewNotes(notes);
        report.setActionTaken(action);
        report.setReviewedAt(LocalDateTime.now());

        Report resolved = reportRepository.save(report);

        // Notify reporter about resolution
        notificationService.createReportResolvedNotification(
                report.getReportedBy().getId(),
                reportId,
                action
        );

        // Take action based on decision
        executeReportAction(report, action);

        return mapToReportResponse(resolved);
    }

    @Transactional
    public ReportResponse dismissReport(Long reportId, Long adminId, String reason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(Report.ReportStatus.DISMISSED);
        report.setReviewedById(adminId);
        report.setReviewNotes(reason);
        report.setReviewedAt(LocalDateTime.now());

        Report dismissed = reportRepository.save(report);

        // Notify reporter that report was dismissed
        notificationService.createReportDismissedNotification(
                report.getReportedBy().getId(),
                reportId
        );

        return mapToReportResponse(dismissed);
    }

    @Transactional
    public ReportResponse escalateReport(Long reportId, Long adminId, String notes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(Report.ReportStatus.ESCALATED);
        report.setReviewedById(adminId);
        report.setReviewNotes(notes);
        report.setReviewedAt(LocalDateTime.now());

        Report escalated = reportRepository.save(report);

        // Notify senior admins
        notificationService.createReportEscalatedNotification(reportId);

        return mapToReportResponse(escalated);
    }

    public ReportResponse getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToReportResponse(report);
    }

    public Page<ReportResponse> getPendingReports(Pageable pageable) {
        Page<Report> reports = reportRepository.findByStatus(Report.ReportStatus.PENDING, pageable);
        return reports.map(this::mapToReportResponse);
    }

    public Page<ReportResponse> getReportsByStatus(Report.ReportStatus status, Pageable pageable) {
        Page<Report> reports = reportRepository.findByStatus(status, pageable);
        return reports.map(this::mapToReportResponse);
    }

    public Page<ReportResponse> getReportsByContentType(Report.ContentType contentType, Pageable pageable) {
        Page<Report> reports = reportRepository.findByContentType(contentType, pageable);
        return reports.map(this::mapToReportResponse);
    }

    public Page<ReportResponse> getUserReports(Long userId, Pageable pageable) {
        Page<Report> reports = reportRepository.findByReportedById(userId, pageable);
        return reports.map(this::mapToReportResponse);
    }

    public Page<ReportResponse> getReportsAgainstUser(Long userId, Pageable pageable) {
        Page<Report> reports = reportRepository.findByReportedUserId(userId, pageable);
        return reports.map(this::mapToReportResponse);
    }

    public long getReportCountByContent(Report.ContentType contentType, String contentId) {
        switch (contentType) {
            case POST:
                return reportRepository.countByPostIdAndStatus(contentId, Report.ReportStatus.RESOLVED);
            case REEL:
                return reportRepository.countByReelIdAndStatus(contentId, Report.ReportStatus.RESOLVED);
            case COMMENT:
                return reportRepository.countByCommentIdAndStatus(contentId, Report.ReportStatus.RESOLVED);
            default:
                return 0;
        }
    }

    private void validateReportRequest(ReportRequest request) {
        int contentCount = 0;
        if (request.getPostId() != null) contentCount++;
        if (request.getReelId() != null) contentCount++;
        if (request.getCommentId() != null) contentCount++;
        if (request.getCampaignId() != null) contentCount++;
        if (request.getReportedUserId() != null) contentCount++;

        if (contentCount != 1) {
            throw new IllegalArgumentException("Exactly one content ID must be provided");
        }

        // Validate content type matches provided ID
        switch (request.getContentType()) {
            case POST:
                if (request.getPostId() == null) {
                    throw new IllegalArgumentException("Post ID required for POST content type");
                }
                break;
            case REEL:
                if (request.getReelId() == null) {
                    throw new IllegalArgumentException("Reel ID required for REEL content type");
                }
                break;
            case COMMENT:
                if (request.getCommentId() == null) {
                    throw new IllegalArgumentException("Comment ID required for COMMENT content type");
                }
                break;
            case CAMPAIGN:
                if (request.getCampaignId() == null) {
                    throw new IllegalArgumentException("Campaign ID required for CAMPAIGN content type");
                }
                break;
            case PROFILE:
                if (request.getReportedUserId() == null) {
                    throw new IllegalArgumentException("User ID required for PROFILE content type");
                }
                break;
        }
    }

    private boolean isDuplicateReport(ReportRequest request) {
        switch (request.getContentType()) {
            case POST:
                return reportRepository.existsByPostIdAndReportedById(
                        request.getPostId(), request.getReportedByUserId()
                );
            case REEL:
                return reportRepository.existsByReelIdAndReportedById(
                        request.getReelId(), request.getReportedByUserId()
                );
            case COMMENT:
                return reportRepository.existsByCommentIdAndReportedById(
                        request.getCommentId(), request.getReportedByUserId()
                );
            case CAMPAIGN:
                return reportRepository.existsByCampaignIdAndReportedById(
                        request.getCampaignId(), request.getReportedByUserId()
                );
            case PROFILE:
                return reportRepository.existsByReportedUserIdAndReportedById(
                        request.getReportedUserId(), request.getReportedByUserId()
                );
            default:
                return false;
        }
    }

    private void executeReportAction(Report report, String action) {
        // This would integrate with content moderation service
        // For now, just placeholder logic
        switch (action.toUpperCase()) {
            case "CONTENT_REMOVED":
                // Call service to remove content
                removeContent(report);
                break;
            case "WARNING_ISSUED":
                // Send warning to content owner
                issueWarning(report);
                break;
            case "ACCOUNT_SUSPENDED":
                // Suspend user account
                suspendAccount(report);
                break;
            case "NO_ACTION":
                // Do nothing
                break;
            default:
                // Log unknown action
                break;
        }
    }

    private void removeContent(Report report) {
        // Implement content removal logic
        // This would call PostService, ReelService, CommentService, etc.
        switch (report.getContentType()) {
            case POST:
                // postService.deletePost(report.getPostId(), ADMIN_ID);
                break;
            case REEL:
                // reelService.deleteReel(report.getReelId(), ADMIN_ID);
                break;
            case COMMENT:
                // commentService.deleteComment(report.getCommentId(), ADMIN_ID);
                break;
            case CAMPAIGN:
                // campaignService.removeCampaign(report.getCampaign().getId(), ADMIN_ID);
                break;
            case PROFILE:
                // Handle profile reports differently
                break;
        }
    }

    private void issueWarning(Report report) {
        Long targetUserId = getContentOwnerId(report);
        if (targetUserId != null) {
            notificationService.createContentWarningNotification(
                    targetUserId,
                    report.getContentType().name(),
                    report.getReason().name()
            );
        }
    }

    private void suspendAccount(Report report) {
        if (report.getReportedUser() != null) {
            // profileService.suspendAccount(report.getReportedUser().getId(), 7); // 7 days
            notificationService.createAccountSuspensionNotification(
                    report.getReportedUser().getId(),
                    7,
                    report.getReason().name()
            );
        }
    }

    private Long getContentOwnerId(Report report) {
        switch (report.getContentType()) {
            case POST:
                // return postService.getPostOwnerId(report.getPostId());
                return null; // Implement in PostService
            case REEL:
                // return reelService.getReelOwnerId(report.getReelId());
                return null; // Implement in ReelService
            case COMMENT:
                // return commentService.getCommentOwnerId(report.getCommentId());
                return null; // Implement in CommentService
            case CAMPAIGN:
                return report.getCampaign().getCreator().getId();
            case PROFILE:
                return report.getReportedUser().getId();
            default:
                return null;
        }
    }

    private ReportResponse mapToReportResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReportedByUserId(report.getReportedBy().getId());
        response.setReportedByUsername(report.getReportedBy().getUser().getUsername());
        response.setContentType(report.getContentType().name());
        response.setReason(report.getReason().name());
        response.setDescription(report.getDescription());
        response.setStatus(report.getStatus().name());
        response.setPostId(report.getPostId());
        response.setReelId(report.getReelId());
        response.setCommentId(report.getCommentId());

        if (report.getCampaign() != null) {
            response.setCampaignId(report.getCampaign().getId());
        }

        if (report.getReportedUser() != null) {
            response.setReportedUserId(report.getReportedUser().getId());
            response.setReportedUsername(report.getReportedUser().getUser().getUsername());
        }

        response.setActionTaken(report.getActionTaken());
        response.setReviewNotes(report.getReviewNotes());
        response.setReviewedAt(report.getReviewedAt());
        response.setCreatedAt(report.getCreatedAt());

        return response;
    }
}