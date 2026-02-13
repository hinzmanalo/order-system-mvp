# Feature 06: Auth Backend

**Priority**: Core
**Dependencies**: 05-common-module
**Parallel with**: None
**Blocks**: 07-catalog-backend, 09-orders-backend, 12-frontend-core

---

## Overview

Implement the authentication and authorization system: user registration, login with JWT tokens, token refresh with rotation, Spring Security filter chain, role-based access control, and admin user management endpoints.

## User Stories

- US-001: User registration
- US-002: User login
- US-003: Token refresh
- US-004: View own profile
- US-021: Admin list users
- US-022: Admin promote user

## Tasks

### 6.1 Entities

- [x] `com.orderhub.auth.entity.Role.java` — enum: `USER`, `ADMIN`
- [x] `com.orderhub.auth.entity.User.java` — JPA entity:
  - `@Table(name = "users")`
  - `id` UUID, `@Id @GeneratedValue(strategy = GenerationType.UUID)`
  - `email` String, `@Column(unique = true, nullable = false)`
  - `passwordHash` String, `@Column(name = "password_hash", nullable = false)`
  - `firstName`, `lastName` String
  - `role` Role, `@Enumerated(EnumType.STRING)`
  - `createdAt`, `updatedAt` LocalDateTime with `@CreationTimestamp`, `@UpdateTimestamp`
- [x] `com.orderhub.auth.entity.RefreshToken.java` — JPA entity:
  - `@Table(name = "refresh_tokens")`
  - `id` UUID
  - `user` User, `@ManyToOne(fetch = LAZY)`
  - `token` String, `@Column(unique = true, nullable = false)`
  - `expiresAt` LocalDateTime
  - `createdAt` LocalDateTime

### 6.2 Repositories

- [x] `com.orderhub.auth.repository.UserRepository.java`:
  - `Optional<User> findByEmail(String email)`
  - `boolean existsByEmail(String email)`
  - Extends `JpaRepository<User, UUID>`
- [x] `com.orderhub.auth.repository.RefreshTokenRepository.java`:
  - `Optional<RefreshToken> findByToken(String token)`
  - `void deleteByUser(User user)`
  - `void deleteByExpiresAtBefore(LocalDateTime now)` (cleanup)

### 6.3 JWT infrastructure

- [x] `com.orderhub.auth.security.JwtTokenProvider.java`:
  - Inject `@Value("${app.jwt.secret}")` and expiry configs
  - `generateAccessToken(User user)` → String JWT (claims: sub=userId, email, role; exp=15min)
  - `generateRefreshToken()` → String (secure random base64)
  - `validateToken(String token)` → boolean
  - `getUserIdFromToken(String token)` → UUID
  - `getEmailFromToken(String token)` → String
  - Uses `io.jsonwebtoken.Jwts` with HS256 signing

### 6.4 Spring Security

- [x] `com.orderhub.auth.security.CustomUserDetailsService.java`:
  - Implements `UserDetailsService`
  - `loadUserByUsername(String email)` → loads User entity, returns Spring `User` with role authority
- [x] `com.orderhub.auth.security.JwtAuthenticationFilter.java`:
  - Extends `OncePerRequestFilter`
  - Extract Bearer token from `Authorization` header
  - Validate token via `JwtTokenProvider`
  - Load user via `CustomUserDetailsService`
  - Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
  - Skip filter for public endpoints
- [x] `com.orderhub.auth.security.JwtAuthEntryPoint.java`:
  - Implements `AuthenticationEntryPoint`
  - Returns 401 JSON response with RFC 7807 structure
- [x] `com.orderhub.auth.security.SecurityConfig.java`:
  - `@EnableWebSecurity`, `@EnableMethodSecurity`
  - `SecurityFilterChain` bean:
    - Public endpoints: `POST /api/v1/auth/**`, `GET /api/v1/products/**`, `GET /swagger-ui/**`, `GET /v3/api-docs/**`, `GET /actuator/health`
    - Admin endpoints: `/api/v1/admin/**` requires `ROLE_ADMIN`
    - All other `/api/v1/**` requires authentication
  - CSRF disabled, session management STATELESS
  - Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
  - `PasswordEncoder` bean: `BCryptPasswordEncoder`
  - `AuthenticationManager` bean

### 6.5 DTOs

- [x] `RegisterRequest.java` — `@NotBlank` email (`@Email`), password (`@Size(min=6)`), firstName, lastName
- [x] `LoginRequest.java` — `@NotBlank` email, password
- [x] `RefreshRequest.java` — `@NotBlank` refreshToken
- [x] `TokenResponse.java` — accessToken, refreshToken, tokenType ("Bearer"), expiresIn (900)
- [x] `UserResponse.java` — id, email, firstName, lastName, role, createdAt
- [x] `UpdateRoleRequest.java` — `@NotNull` role (String)

### 6.6 Service

- [x] `com.orderhub.auth.service.AuthService.java` — interface:
  - `register(RegisterRequest)` → UserResponse
  - `login(LoginRequest)` → TokenResponse
  - `refresh(RefreshRequest)` → TokenResponse
  - `getCurrentUser(UUID userId)` → UserResponse
  - `getAllUsers(Pageable)` → Page\<UserResponse\>
  - `getUserById(UUID userId)` → UserResponse
  - `updateUserRole(UUID userId, UpdateRoleRequest)` → UserResponse
- [x] `com.orderhub.auth.service.AuthServiceImpl.java`:
  - **register**: check `existsByEmail`, hash password with BCrypt, save User, return UserResponse
  - **login**: find user by email, verify password with `passwordEncoder.matches()`, generate access + refresh tokens, save RefreshToken entity, return TokenResponse
  - **refresh**: find RefreshToken by token string, check expiry, delete old token, generate new pair, save new RefreshToken, return TokenResponse
  - **getCurrentUser**: find User by ID, map to UserResponse
  - **updateUserRole**: find User, set new role, save

### 6.7 Controllers

- [x] `com.orderhub.auth.controller.AuthController.java`:
  - `POST /api/v1/auth/register` → 201 + UserResponse
  - `POST /api/v1/auth/login` → 200 + TokenResponse
  - `POST /api/v1/auth/refresh` → 200 + TokenResponse
  - `GET /api/v1/auth/me` → 200 + UserResponse (extract userId from `SecurityContextHolder`)
- [x] `com.orderhub.auth.controller.AdminUserController.java`:
  - `GET /api/v1/admin/users` → 200 + Page\<UserResponse\>
  - `GET /api/v1/admin/users/{id}` → 200 + UserResponse
  - `PUT /api/v1/admin/users/{id}/role` → 200 + UserResponse

## Verification

- [x] `POST /api/v1/auth/register` with valid data → 201, user returned without password
- [x] `POST /api/v1/auth/register` with duplicate email → 409
- [x] `POST /api/v1/auth/register` with invalid data → 400 with validation details
- [x] `POST /api/v1/auth/login` with correct credentials → 200 with tokens
- [x] `POST /api/v1/auth/login` with wrong password → 401
- [x] `GET /api/v1/auth/me` with valid Bearer token → 200
- [x] `GET /api/v1/auth/me` without token → 401
- [x] `POST /api/v1/auth/refresh` with valid refresh token → 200 with new tokens
- [x] `POST /api/v1/auth/refresh` with old (rotated) token → 401
- [x] `GET /api/v1/admin/users` as ADMIN → 200
- [x] `GET /api/v1/admin/users` as USER → 403
- [x] `PUT /api/v1/admin/users/{id}/role` promotes USER to ADMIN

## Code Quality Enhancements

- [x] **SLF4J Logging**:
  - Added logger declarations to all service, controller, and security classes
  - Implemented parameterized logging for performance (e.g., `logger.info("User {} logged in", userId)`)
  - Used appropriate log levels:
    - INFO: Business events (registration, login, role changes)
    - WARN: Failed operations (invalid credentials, duplicate emails, unauthorized access)
    - DEBUG: Read operations and routine processing
  - Never log sensitive data (passwords, tokens)
  - Added context (user IDs, email addresses) to all log messages

- [x] **JavaDoc Documentation**:
  - Added comprehensive class-level JavaDoc with `@author`, `@version`, `@since` tags
  - Documented all public methods with `@param`, `@return`, `@throws` tags
  - Included thread-safety notes where applicable
  - Added business logic explanations in class descriptions
  - Documented security considerations (e.g., password hashing, token rotation)
  - Enhanced DTOs, entities, and repositories with descriptive documentation

## Files Created

```
backend/src/main/java/com/orderhub/auth/
├── controller/
│   ├── AuthController.java
│   └── AdminUserController.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── RefreshRequest.java
│   ├── TokenResponse.java
│   ├── UserResponse.java
│   └── UpdateRoleRequest.java
├── entity/
│   ├── User.java
│   ├── Role.java
│   └── RefreshToken.java
├── repository/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── service/
│   ├── AuthService.java
│   └── AuthServiceImpl.java
└── security/
    ├── SecurityConfig.java
    ├── JwtAuthenticationFilter.java
    ├── JwtTokenProvider.java
    ├── CustomUserDetailsService.java
    └── JwtAuthEntryPoint.java
```
