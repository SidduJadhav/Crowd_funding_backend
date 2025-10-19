package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Existing method (keep this)
    boolean existsByReelIdAndReportedBy_Id(String reelId, Long reportedById);

    // NEW METHODS NEEDED:

    // Find reports
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    Page<Report> findByContentType(Report.ContentType contentType, Pageable pageable);
    Page<Report> findByReportedById(Long reportedById, Pageable pageable);
    Page<Report> findByReportedUserId(Long reportedUserId, Pageable pageable);

    // Check for duplicates
    boolean existsByPostIdAndReportedById(String postId, Long reportedById);
    boolean existsByReelIdAndReportedById(String reelId, Long reportedById);
    boolean existsByCommentIdAndReportedById(String commentId, Long reportedById);
    boolean existsByCampaignIdAndReportedById(Long campaignId, Long reportedById);
    boolean existsByReportedUserIdAndReportedById(Long reportedUserId, Long reportedById);

    // Count reports
    long countByPostIdAndStatus(String postId, Report.ReportStatus status);
    long countByReelIdAndStatus(String reelId, Report.ReportStatus status);
    long countByCommentIdAndStatus(String commentId, Report.ReportStatus status);
}