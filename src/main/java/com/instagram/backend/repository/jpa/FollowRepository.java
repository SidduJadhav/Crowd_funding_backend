package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Follow;
import com.instagram.backend.model.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // Existing methods (keep these)
    Optional<Follow> findByFollowerAndFollowing(Profile follower, Profile following);
    List<Follow> findByFollowing(Profile following);
    List<Follow> findByFollower(Profile follower);
    int countByFollowing(Profile following);
    int countByFollower(Profile follower);

    // NEW METHODS NEEDED:
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // For status filtering
    List<Follow> findByFollowingIdAndStatus(Long followingId, Follow.FollowStatus status);
    List<Follow> findByFollowerIdAndStatus(Long followerId, Follow.FollowStatus status);
    Optional<Follow> findByFollowerIdAndFollowingIdAndStatus(
            Long followerId, Long followingId, Follow.FollowStatus status
    );
    boolean existsByFollowerIdAndFollowingIdAndStatus(
            Long followerId, Long followingId, Follow.FollowStatus status
    );

    // For counts
    int countByFollowingIdAndStatus(Long followingId, Follow.FollowStatus status);
    int countByFollowerIdAndStatus(Long followerId, Follow.FollowStatus status);
}