package com.instagram.backend.repository.jpa;

import com.instagram.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByIdAndRole(Long id, String role);

    /**
     * Find users by role
     */
    List<User> findByRole(String role);

    /**
     * Find active users by role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveByRole(@Param("role") String role);

    /**
     * Find verified users
     */
    List<User> findByIsVerifiedTrue();

    /**
     * Find users by verification status
     */
    List<User> findByIsVerified(Boolean isVerified);

    /**
     * Find users by active status
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * Find recently active users
     */
    @Query("SELECT u FROM User u WHERE u.lastActiveAt IS NOT NULL AND u.lastActiveAt >= :since AND u.isActive = true")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since);

    /**
     * Count users by role
     */
    long countByRole(String role);

    /**
     * Find users with pending password reset tokens
     */
    @Query("SELECT u FROM User u WHERE u.passwordResetToken IS NOT NULL AND u.passwordResetTokenExpiry > :now")
    List<User> findUsersWithValidResetTokens(@Param("now") LocalDateTime now);

    /**
     * Find user by password reset token
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Find locked accounts
     */
    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > :now")
    List<User> findLockedAccounts(@Param("now") LocalDateTime now);
}