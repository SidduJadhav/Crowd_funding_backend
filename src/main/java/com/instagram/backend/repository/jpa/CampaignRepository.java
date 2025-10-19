package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Campaign;
import com.instagram.backend.model.entity.Campaign.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);

    Page<Campaign> findByCategory(String category, Pageable pageable);

    Page<Campaign> findByCreatorId(Long creatorId, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' ORDER BY c.createdAt DESC")
    List<Campaign> findActiveCampaigns(Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.endDate > :currentDate")
    Page<Campaign> findActiveAndNotExpired(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.isVerified = true ORDER BY c.donorCount DESC")
    Page<Campaign> findTrendingCampaigns(Pageable pageable);

    long countByCreatorIdAndStatus(Long creatorId, CampaignStatus status);
}
