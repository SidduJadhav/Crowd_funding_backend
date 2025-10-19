package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Like;
import com.instagram.backend.model.entity.Like.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndPostId(Long userId, String postId);

    Optional<Like> findByUserIdAndReelId(Long userId, String reelId);

    Optional<Like> findByUserIdAndCampaignId(Long userId, Long campaignId);

    long countByPostId(String postId);

    long countByReelId(String reelId);

    long countByCampaignId(Long campaignId);

    boolean existsByUserIdAndPostId(Long userId, String postId);

    boolean existsByUserIdAndReelId(Long userId, String reelId);

    boolean existsByUserIdAndCampaignId(Long userId, Long campaignId);

    @Query("SELECT l.user.id FROM Like l WHERE l.postId = :postId")
    List<Long> findUserIdsByPostId(@Param("postId") String postId);

    @Query("SELECT l.user.id FROM Like l WHERE l.reelId = :reelId")
    List<Long> findUserIdsByReelId(@Param("reelId") String reelId);
}
