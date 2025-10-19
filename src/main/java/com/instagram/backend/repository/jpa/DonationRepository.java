package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Donation;
import com.instagram.backend.model.entity.Donation.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Page<Donation> findByCampaignId(Long campaignId, Pageable pageable);

    // FIXED: Changed return type to List for service compatibility
    List<Donation> findByCampaignId(Long campaignId);

    Page<Donation> findByDonorId(Long donorId, Pageable pageable);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalDonationsByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED'")
    Integer getUniqueDonorCount(@Param("campaignId") Long campaignId);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.donor.id = :userId AND d.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalDonationsByUser(@Param("userId") Long userId);

    @Query("SELECT d FROM Donation d WHERE d.campaign.id = :campaignId AND d.paymentStatus = :status ORDER BY d.createdAt DESC")
    Page<Donation> findByCampaignIdAndStatus(@Param("campaignId") Long campaignId, @Param("status") PaymentStatus status, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.donor.id = :userId AND d.campaign.id = :campaignId AND d.paymentStatus = 'COMPLETED'")
    long countByDonorIdAndCampaignId(@Param("userId") Long userId, @Param("campaignId") Long campaignId);

    @Query("SELECT d FROM Donation d WHERE d.createdAt BETWEEN :startDate AND :endDate AND d.paymentStatus = 'COMPLETED'")
    List<Donation> findDonationsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // FIXED: Added method to find by transaction ID
    Donation findByTransactionId(String transactionId);

    // FIXED: Added method to find completed donations by campaign
    List<Donation> findByCampaignIdAndPaymentStatus(Long campaignId, PaymentStatus paymentStatus);
}