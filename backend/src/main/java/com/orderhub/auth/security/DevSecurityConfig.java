package com.orderhub.auth.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Development-only security configuration that permits all HTTP requests
 * without JWT authentication.
 * <p>
 * This configuration is activated by the {@code nosecurity} Spring profile
 * and replaces the standard {@link SecurityConfig}. It disables CSRF,
 * uses stateless sessions, and permits all requests. A
 * {@link DevAuthenticationFilter} is registered to inject a mock ADMIN user
 * into the SecurityContext so that {@code @PreAuthorize} annotations
 * continue to work.
 * </p>
 * <p>
 * WARNING: This configuration must never be active in production environments.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 * @see DevAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("nosecurity")
public class DevSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(DevSecurityConfig.class);

    private final DevAuthenticationFilter devAuthenticationFilter;

    /**
     * Constructs a DevSecurityConfig with the dev authentication filter.
     *
     * @param devAuthenticationFilter the filter that injects mock ADMIN user
     */
    public DevSecurityConfig(DevAuthenticationFilter devAuthenticationFilter) {
        this.devAuthenticationFilter = devAuthenticationFilter;
    }

    /**
     * Configures a permissive security filter chain that allows all requests.
     * <p>
     * Registers the {@link DevAuthenticationFilter} to inject a mock ADMIN user
     * so that {@code @PreAuthorize("hasRole('ADMIN')")} annotations work without
     * real authentication.
     * </p>
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.warn("SECURITY DISABLED — nosecurity profile is active. DO NOT use in production!");

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());

        // Inject mock admin user so @PreAuthorize("hasRole('ADMIN')") works
        http.addFilterBefore(devAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures the password encoder using BCrypt.
     * <p>
     * Required even in dev mode for any bean that depends on PasswordEncoder.
     * </p>
     *
     * @return the BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
