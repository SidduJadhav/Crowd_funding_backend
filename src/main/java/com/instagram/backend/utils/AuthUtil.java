package com.instagram.backend.utils;

import com.instagram.backend.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthUtil {

    public static UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserDetailsImpl) {
                return (UserDetailsImpl) authentication.getPrincipal();
            }
        }

        return null;
    }

    public static Long getCurrentUserId() {
        UserDetailsImpl user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getCurrentUsername() {
        UserDetailsImpl user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    public static boolean isCurrentUserAdmin() {
        UserDetailsImpl user = getCurrentUser();
        if (user != null) {
            return user.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") ||
                            auth.getAuthority().equals("ROLE_SENIOR_ADMIN"));
        }
        return false;
    }

    public static boolean isCurrentUserAuthor(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    public static boolean hasRole(String role) {
        UserDetailsImpl user = getCurrentUser();
        if (user != null) {
            return user.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }
}