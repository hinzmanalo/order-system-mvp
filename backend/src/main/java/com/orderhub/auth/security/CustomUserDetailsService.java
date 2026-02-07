package com.orderhub.auth.security;

import com.orderhub.auth.entity.User;
import com.orderhub.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for loading user information by email.
 * <p>
 * This service integrates with Spring Security to provide user authentication
 * details. Users are loaded from the database by their email address.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    /**
     * Constructs a CustomUserDetailsService with the user repository.
     *
     * @param userRepository the repository for accessing user data
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their email address for Spring Security authentication.
     * <p>
     * The user's role is prefixed with "ROLE_" to match Spring Security conventions.
     * </p>
     *
     * @param email the email address identifying the user
     * @return UserDetails containing the user's authentication information
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user details for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        logger.debug("User found with email: {}, role: {}", email, user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
