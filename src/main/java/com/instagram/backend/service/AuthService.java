package com.instagram.backend.service;

import com.instagram.backend.dto.request.LoginRequest;
import com.instagram.backend.dto.request.SignupRequest;
import com.instagram.backend.dto.response.JwtAuthenticationResponse;
import com.instagram.backend.model.entity.Profile;
import com.instagram.backend.model.entity.User;
import com.instagram.backend.repository.jpa.ProfileRepository;
import com.instagram.backend.repository.jpa.UserRepository;
import com.instagram.backend.security.JwtTokenProvider;
import com.instagram.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long ACCOUNT_LOCK_DURATION_MINUTES = 30;

    /**
     * Register a new user with profile creation
     * @param signupRequest Registration data
     */
    @Transactional
    public void registerUser(SignupRequest signupRequest) {
        log.info("Attempting to register user: {}", signupRequest.getUsername());

        // Validate username
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            log.warn("Registration failed: Username already taken - {}", signupRequest.getUsername());
            throw new RuntimeException("Username is already taken");
        }

        // Validate email
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            log.warn("Registration failed: Email already in use - {}", signupRequest.getEmail());
            throw new RuntimeException("Email is already in use");
        }

        // Validate passwords match
        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            log.warn("Registration failed: Passwords do not match - {}", signupRequest.getUsername());
            throw new RuntimeException("Passwords do not match");
        }

        // Validate password strength (optional but recommended)
        if (signupRequest.getPassword().length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }

        try {
            // Create User entity
            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setRole("USER");
            user.setIsActive(true);
            user.setIsVerified(false);
            user.setFailedLoginAttempts(0);

            User savedUser = userRepository.save(user);
            log.info("User created successfully: {}", savedUser.getUsername());

            // Create Profile entity
            Profile profile = new Profile();
            profile.setUser(savedUser);
            profile.setName("not yet set");
            profile.setIsPrivate(false);
            profile.setFollowersCount(0L);
            profile.setFollowingCount(0L);
            profile.setPostsCount(0L);
            profile.setEmailNotificationsEnabled(true);
            profile.setPushNotificationsEnabled(true);
            profile.setSmsNotificationsEnabled(false);


            profileRepository.save(profile);
            log.info("Profile created for user: {}", savedUser.getUsername());

        } catch (Exception e) {
            log.error("Error during user registration", e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticate user and generate JWT tokens
     * @param loginRequest Login credentials
     * @return JWT authentication response
     */
    public JwtAuthenticationResponse loginUser(LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            // Check if account is locked
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> {
                        log.warn("Login failed: User not found - {}", loginRequest.getUsername());
                        throw new BadCredentialsException("Invalid username or password");
                    });

            if (isAccountLocked(user)) {
                log.warn("Login failed: Account locked for user - {}", loginRequest.getUsername());
                throw new RuntimeException("Account is locked. Please try again later.");
            }

            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

            log.info("User logged in successfully: {}", loginRequest.getUsername());

            return JwtAuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .expiresIn(86400) // 24 hours in seconds
                    .build();

        } catch (BadCredentialsException e) {
            // Handle failed login attempt
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                incrementFailedLoginAttempts(user);
            }
            log.warn("Login failed: Invalid credentials for user - {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password", e);
        }
    }

    /**
     * Refresh JWT access token using refresh token
     * @param token Refresh token
     * @return New JWT tokens
     */
    public JwtAuthenticationResponse refreshToken(String token) {
        log.debug("Attempting to refresh token");

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("Token refresh failed: Invalid or expired token");
            throw new RuntimeException("Invalid or expired refresh token");
        }

        try {
            Long userId = jwtTokenProvider.getUserIdFromJwt(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getIsActive()) {
                log.warn("Token refresh failed: User account inactive - {}", user.getUsername());
                throw new RuntimeException("User account is inactive");
            }

            List<String> roles = List.of(user.getRole());
            String newAccessToken = jwtTokenProvider.generateAccessTokenFromUserId(
                    user.getId(),
                    user.getUsername(),
                    roles
            );
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

            log.info("Token refreshed successfully for user: {}", user.getUsername());

            return JwtAuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .expiresIn(86400)
                    .build();

        } catch (Exception e) {
            log.error("Error refreshing token", e);
            throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
        }
    }

    /**
     * Change user password
     * @param userId User ID
     * @param oldPassword Current password
     * @param newPassword New password
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Password change requested for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Password change failed: Invalid current password for user - {}", user.getUsername());
            throw new RuntimeException("Invalid current password");
        }

        // Validate new password
        if (newPassword.length() < 8) {
            throw new RuntimeException("New password must be at least 8 characters long");
        }

        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("New password cannot be the same as old password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUsername());
    }

    /**
     * Request password reset
     * @param email User email
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset failed: Email not found - {}", email);
                    throw new RuntimeException("No account found with this email");
                });

        // Generate reset token (in production, use a secure token)
        String resetToken = java.util.UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // TODO: Send reset email to user
        log.info("Password reset token generated for user: {}", user.getUsername());
    }

    /**
     * Reset password with token
     * @param token Reset token
     * @param newPassword New password
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Password reset attempt with token");

        User user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getPasswordResetToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        // Validate token expiry
        if (user.getPasswordResetTokenExpiry() == null ||
                user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed: Token expired for user - {}", user.getUsername());
            throw new RuntimeException("Reset token has expired");
        }

        // Validate password
        if (newPassword.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }

        // Reset password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setLastPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    /**
     * Check if account is locked
     */
    private boolean isAccountLocked(User user) {
        return user.getAccountLockedUntil() != null &&
                user.getAccountLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Increment failed login attempts and lock account if needed
     */
    @Transactional
    private void incrementFailedLoginAttempts(User user) {
        int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        attempts++;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLockedUntil(
                    LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_DURATION_MINUTES)
            );
            log.warn("Account locked for user: {} after {} failed attempts",
                    user.getUsername(), attempts);
        }

        userRepository.save(user);
    }

    /**
     * Verify user email (for email verification flow)
     */
    @Transactional
    public void verifyEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getUsername());
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("Account deactivated for user: {}", user.getUsername());
    }

    /**
     * Reactivate user account
     */
    @Transactional
    public void reactivateAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        log.info("Account reactivated for user: {}", user.getUsername());
    }
}