package com.instagram.backend.service;

import com.instagram.backend.model.entity.User;
import com.instagram.backend.repository.jpa.UserRepository;
import com.instagram.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username for Spring Security authentication
     * @param username The username to look up
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found or account is disabled
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    throw new UsernameNotFoundException("User not found with username: " + username);
                });

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Login attempt for disabled account: {}", username);
            throw new UsernameNotFoundException("User account is disabled");
        }

        // Check if account is locked
        if (isAccountLocked(user)) {
            log.warn("Login attempt for locked account: {}", username);
            throw new UsernameNotFoundException("User account is locked. Please try again later.");
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        log.debug("User details loaded successfully for: {}", username);

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }

    /**
     * Load user by ID for token-based authentication
     * @param id The user ID to look up
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found or account is disabled
     */
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        log.debug("Loading user details for ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    throw new UsernameNotFoundException("User not found with id: " + id);
                });

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Access attempt for disabled account: {}", user.getUsername());
            throw new UsernameNotFoundException("User account is disabled");
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        log.debug("User details loaded successfully for ID: {}", id);

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }

    /**
     * Check if user account is locked due to failed login attempts
     * @param user The user to check
     * @return true if account is locked, false otherwise
     */
    private boolean isAccountLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }

        if (user.getAccountLockedUntil().isBefore(LocalDateTime.now())) {
            // Lock period has expired, unlock the account
            log.info("Unlocking account for user: {}", user.getUsername());
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            return false;
        }

        return true;
    }
}