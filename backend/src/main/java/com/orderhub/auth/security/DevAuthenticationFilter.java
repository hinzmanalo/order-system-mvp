package com.orderhub.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Development-only authentication filter that auto-injects a mock ADMIN user
 * into the SecurityContext for every request.
 * <p>
 * This allows all {@code @PreAuthorize("hasRole('ADMIN')")} annotations to pass
 * without requiring JWT authentication. Only active when the {@code nosecurity}
 * Spring profile is enabled.
 * </p>
 * <p>
 * WARNING: This filter must never be active in production environments.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@Profile("nosecurity")
public class DevAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DevAuthenticationFilter.class);

    /**
     * Injects a mock ADMIN user into the SecurityContext if no authentication
     * is already present.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));

            var auth = new UsernamePasswordAuthenticationToken(
                    "dev-admin@orderhub.local", // principal
                    null, // credentials
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.debug("Dev auth filter: injected mock ADMIN user for {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
