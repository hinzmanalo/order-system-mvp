package com.orderhub.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT authentication entry point for handling unauthenticated access attempts.
 * <p>
 * This component is invoked when an unauthenticated user attempts to access
 * a protected resource. It returns a standard RFC 7807 ProblemDetail response
 * with HTTP 401 status.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthEntryPoint.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles authentication failures by returning a 401 Unauthorized response.
     *
     * @param request       the HTTP request that resulted in an AuthenticationException
     * @param response      the HTTP response to send
     * @param authException the exception that was thrown during authentication
     * @throws IOException      if an I/O error occurs during response writing
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        logger.warn("Unauthorized access attempt to {} - {}",
                request.getRequestURI(), authException.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(401);
        problemDetail.setTitle("Unauthorized");
        problemDetail.setDetail("Full authentication is required to access this resource");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
