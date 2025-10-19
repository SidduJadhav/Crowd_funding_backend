package com.instagram.backend.repository.mongo;

import com.instagram.backend.model.document.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    // Existing methods (keep these)
    Page<Comment> findByPostIdOrderByCreatedAtDesc(String postId, Pageable pageable);
    Page<Comment> findByReelIdOrderByCreatedAtDesc(String reelId, Pageable pageable);
    int countByPostId(String postId);
    int countByReelId(String reelId);

    // NEW METHODS NEEDED:

    // With soft delete support
    Page<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String postId, Pageable pageable
    );
    Page<Comment> findByReelIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String reelId, Pageable pageable
    );
    Page<Comment> findByCampaignIdAndIsDeletedFalseOrderByCreatedAtDesc(
            Long campaignId, Pageable pageable
    );

    // For replies
    Page<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(
            String parentCommentId, Pageable pageable
    );

    // Counts with soft delete
    long countByPostIdAndIsDeletedFalse(String postId);
    long countByReelIdAndIsDeletedFalse(String reelId);
    long countByCampaignIdAndIsDeletedFalse(Long campaignId);

    // ADDED: Find by campaignId without soft delete for backward compatibility
    Page<Comment> findByCampaignIdOrderByCreatedAtDesc(Long campaignId, Pageable pageable);

    // ADDED: Count by campaignId without soft delete for backward compatibility
    int countByCampaignId(Long campaignId);
}