package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * Find profile by associated user's username
     * Note: Profile entity doesn't have its own username field,
     * so this queries through the User relationship
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.username = :username")
    Optional<Profile> findByUsername(@Param("username") String username);

    /**
     * Check if profile exists by associated user's username
     */
    @Query("SELECT COUNT(p) > 0 FROM Profile p JOIN p.user u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    /**
     * Search profiles by user's username or profile name
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :usernameQuery, '%')) OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :nameQuery, '%'))")
    Page<Profile> findByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(
            @Param("usernameQuery") String usernameQuery,
            @Param("nameQuery") String nameQuery,
            Pageable pageable
    );

    /**
     * Find active profile by ID
     * Note: Profile entity doesn't have isActive field, checking through User relationship
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE p.id = :id AND u.isActive = true")
    Optional<Profile> findActiveById(@Param("id") Long id);

    /**
     * Find profiles by user's role
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.role = :role")
    List<Profile> findByRole(@Param("role") String role);

    /**
     * Find profiles by user's role with pagination
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.role = :role")
    Page<Profile> findByRole(@Param("role") String role, Pageable pageable);

    /**
     * Find senior admin profiles (used in report escalation notifications)
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.role = 'SENIOR_ADMIN' AND u.isActive = true")
    List<Profile> findBySeniorAdminRole();

    /**
     * Find all active admin profiles
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.role IN ('ADMIN', 'SENIOR_ADMIN') AND u.isActive = true")
    List<Profile> findAllActiveAdmins();

    /**
     * Check if profile exists and user is active
     */
    @Query("SELECT COUNT(p) > 0 FROM Profile p JOIN p.user u WHERE p.id = :id AND u.isActive = true")
    boolean existsActiveById(@Param("id") Long id);

    /**
     * Find profiles by multiple IDs (for batch operations)
     */
    List<Profile> findByIdIn(List<Long> ids);

    /**
     * Find profile by user's email
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.email = :email")
    Optional<Profile> findByEmail(@Param("email") String email);

    /**
     * Check if profile exists by user's email
     */
    @Query("SELECT COUNT(p) > 0 FROM Profile p JOIN p.user u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Find profiles that have enabled email notifications
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.isActive = true AND p.emailNotificationsEnabled = true")
    List<Profile> findProfilesWithEmailNotifications();

    /**
     * Find verified profiles with pagination
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.isVerified = true")
    Page<Profile> findByIsVerifiedTrue(Pageable pageable);

    /**
     * Find unverified profiles with pagination
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.isVerified = false")
    Page<Profile> findByIsVerifiedFalse(Pageable pageable);

    /**
     * Count profiles by user's role
     */
    @Query("SELECT COUNT(p) FROM Profile p JOIN p.user u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    /**
     * Check if profile exists by user ID and role
     */
    @Query("SELECT COUNT(p) > 0 FROM Profile p JOIN p.user u WHERE p.id = :id AND u.role = :role")
    boolean existsByIdAndRole(@Param("id") Long id, @Param("role") String role);

    /**
     * Find recently active profiles
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.lastActiveAt IS NOT NULL AND u.lastActiveAt >= :since AND u.isActive = true")
    List<Profile> findRecentlyActiveProfiles(@Param("since") LocalDateTime since);

    /**
     * Find private profiles
     */
    List<Profile> findByIsPrivateTrue();

    /**
     * Find public profiles
     */
    List<Profile> findByIsPrivateFalse();

    /**
     * Find profiles with pagination sorted by followers
     */
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.isActive = true ORDER BY p.followersCount DESC")
    Page<Profile> findAllActiveOrderByFollowersDesc(Pageable pageable);

    /**
     * Find profile by user ID
     */
    @Query("SELECT p FROM Profile p WHERE p.user.id = :userId")
    Optional<Profile> findByUserId(@Param("userId") Long userId);
    boolean existsByUser(User user);

}