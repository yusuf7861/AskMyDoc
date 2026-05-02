package com.askmydoc.askmydoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline Spring Security configuration for a stateless REST API.
 *
 * CSRF is disabled intentionally: this API is stateless (no sessions or cookies are used),
 * so CSRF attacks are not applicable. The {@link SessionCreationPolicy#STATELESS} setting
 * enforces this contract. Replace the {@code permitAll()} rule with proper API-key or
 * OAuth2 authentication before deploying to production.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Safe: stateless REST, no session/cookie auth
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
