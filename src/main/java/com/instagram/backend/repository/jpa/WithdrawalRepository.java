package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Withdrawal;
import com.instagram.backend.model.entity.Withdrawal.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Page<Withdrawal> findByCampaignId(Long campaignId, Pageable pageable);

    Page<Withdrawal> findByRequesterId(Long requesterId, Pageable pageable);

    Page<Withdrawal> findByStatus(WithdrawalStatus status, Pageable pageable);

    @Query("SELECT SUM(w.amount) FROM Withdrawal w WHERE w.campaign.id = :campaignId AND w.status = 'COMPLETED'")
    BigDecimal getTotalWithdrawnByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(w) FROM Withdrawal w WHERE w.campaign.id = :campaignId AND w.status IN ('PENDING', 'PROCESSING')")
    long countPendingWithdrawals(@Param("campaignId") Long campaignId);

    @Query("SELECT w FROM Withdrawal w WHERE w.status = :status ORDER BY w.createdAt ASC")
    Page<Withdrawal> findByStatusOrderByCreatedAtAsc(@Param("status") WithdrawalStatus status, Pageable pageable);

    boolean existsByCampaignIdAndStatusIn(Long campaignId, List<WithdrawalStatus> statuses);
}
