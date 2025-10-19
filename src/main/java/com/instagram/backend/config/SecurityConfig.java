package com.instagram.backend.config;

import com.instagram.backend.security.JwtAuthenticationEntryPoint;
import com.instagram.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/campaigns/active", "/api/v1/campaigns/category/**").permitAll()
                        .requestMatchers("/api/v1/search/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Admin endpoints - require ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/v1/campaigns/{id}/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/withdrawals/{id}/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/withdrawals/{id}/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bank-accounts/{id}/verify").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports/{id}/review").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports/{id}/resolve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports/{id}/dismiss").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports/{id}/escalate").hasRole("ADMIN")
                        .requestMatchers("/api/v1/reports/pending", "/api/v1/reports/status/{status}").hasRole("ADMIN")
                        .requestMatchers("/api/v1/withdrawals/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/donations/{id}/refund").hasRole("ADMIN")

                        // User endpoints - require authentication
                        .requestMatchers(HttpMethod.POST, "/api/v1/campaigns").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/campaigns/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/campaigns/{id}/publish").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/campaigns/{id}/pause").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/campaigns/{id}/resume").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/campaign-updates").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/campaign-updates/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/campaign-updates/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/donations").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/withdrawals").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bank-accounts").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bank-accounts/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bank-accounts/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/comments").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/comments/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/comments/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/posts").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/posts/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/posts/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reels").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reels/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reels/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/likes").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/likes").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/follows/{id}/follow").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/follows/{id}/unfollow").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/follows/{id}/block").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/follows/{id}/unblock").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/profiles/{id}").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/profiles").authenticated()

                        // Authenticated read endpoints
                        .requestMatchers(HttpMethod.GET).authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}