package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.CampaignUpdate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignUpdateRepository extends JpaRepository<CampaignUpdate, Long> {

    Page<CampaignUpdate> findByCampaignIdOrderByCreatedAtDesc(Long campaignId, Pageable pageable);

    Page<CampaignUpdate> findByCampaignIdAndIsMilestoneTrueOrderByCreatedAtDesc(Long campaignId, Pageable pageable);

    Page<CampaignUpdate> findByCampaignCreatorIdOrderByCreatedAtDesc(Long creatorId, Pageable pageable);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndIsMilestoneTrue(Long campaignId);

    void deleteByCampaignId(Long campaignId);

    @Query("SELECT cu FROM CampaignUpdate cu WHERE cu.campaign.status = 'ACTIVE' ORDER BY cu.createdAt DESC")
    Page<CampaignUpdate> findActiveCampaignUpdates(Pageable pageable);
}