# Authentication Module Documentation

## Overview

The Authentication module provides user registration, authentication, and authorization capabilities for the OrderHub system. It implements JWT-based stateless authentication with refresh token rotation for enhanced security.

---

## How Authentication Works

### Authentication Flow Diagram

```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────┐
│  Client  │     │ AuthController│     │  AuthService    │     │ Database │
└────┬─────┘     └──────┬───────┘     └────────┬────────┘     └────┬─────┘
     │                  │                      │                   │
     │ 1. POST /login   │                      │                   │
     │ (email, password)│                      │                   │
     │─────────────────>│                      │                   │
     │                  │ 2. login(request)    │                   │
     │                  │─────────────────────>│                   │
     │                  │                      │ 3. findByEmail()  │
     │                  │                      │──────────────────>│
     │                  │                      │ 4. User entity    │
     │                  │                      │<──────────────────│
     │                  │                      │                   │
     │                  │     5. Verify password (BCrypt)          │
     │                  │                      │                   │
     │                  │     6. Generate JWT access token         │
     │                  │     7. Generate refresh token            │
     │                  │                      │                   │
     │                  │                      │ 8. Save refresh   │
     │                  │                      │    token          │
     │                  │                      │──────────────────>│
     │                  │                      │                   │
     │                  │ 9. TokenResponse     │                   │
     │                  │<─────────────────────│                   │
     │ 10. Return tokens│                      │                   │
     │<─────────────────│                      │                   │
     │                  │                      │                   │
```

### Request Authentication Flow

```
┌──────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌────────────┐
│  Client  │     │  JwtAuthFilter  │     │ JwtTokenProvider│     │ Controller │
└────┬─────┘     └────────┬────────┘     └────────┬────────┘     └─────┬──────┘
     │                    │                       │                    │
     │ 1. Request with    │                       │                    │
     │ Authorization:     │                       │                    │
     │ Bearer <token>     │                       │                    │
     │───────────────────>│                       │                    │
     │                    │                       │                    │
     │                    │ 2. Extract JWT from   │                    │
     │                    │    Authorization header                    │
     │                    │                       │                    │
     │                    │ 3. validateToken(jwt) │                    │
     │                    │──────────────────────>│                    │
     │                    │                       │                    │
     │                    │   4. Parse & verify   │                    │
     │                    │      signature        │                    │
     │                    │   5. Check expiration │                    │
     │                    │                       │                    │
     │                    │ 6. Return valid/invalid                    │
     │                    │<──────────────────────│                    │
     │                    │                       │                    │
     │                    │ 7. If valid: Extract email, load user      │
     │                    │    Set SecurityContext                     │
     │                    │                       │                    │
     │                    │ 8. Continue filter chain                   │
     │                    │───────────────────────────────────────────>│
     │                    │                       │                    │
     │                    │                       │         9. Process │
     │                    │                       │            request │
     │                    │                       │                    │
     │ 10. Response       │                       │                    │
     │<───────────────────────────────────────────────────────────────│
     │                    │                       │                    │
```

### Step-by-Step Authentication Process

#### 1. User Registration
1. Client sends `POST /api/v1/auth/register` with email, password, firstName, lastName
2. `AuthService.register()` checks if email already exists
3. Password is hashed using BCrypt
4. New User entity created with `ROLE_USER`
5. User saved to database
6. `UserResponse` returned (without password)

#### 2. User Login
1. Client sends `POST /api/v1/auth/login` with email and password
2. `AuthService.login()` finds user by email
3. BCrypt verifies submitted password against stored hash
4. If valid, `JwtTokenProvider.generateAccessToken()` creates JWT containing:
   - `sub`: User ID (UUID)
   - `email`: User's email
   - `role`: User's role
   - `iat`: Issued at timestamp
   - `exp`: Expiration timestamp (15 min from now)
5. Random refresh token generated (48 bytes, Base64 URL-encoded)
6. Refresh token saved to `refresh_tokens` table with 7-day expiry
7. Both tokens returned to client

#### 3. Accessing Protected Endpoints
1. Client includes `Authorization: Bearer <access_token>` header
2. `JwtAuthenticationFilter.doFilterInternal()` intercepts request
3. JWT extracted from header (removes "Bearer " prefix)
4. `JwtTokenProvider.validateToken()` verifies:
   - Signature is valid (using secret key)
   - Token is not expired
   - Token is not malformed
5. If valid, email extracted from token claims
6. `CustomUserDetailsService.loadUserByUsername()` loads user from DB
7. `UsernamePasswordAuthenticationToken` created with user authorities
8. Authentication set in `SecurityContextHolder`
9. Request proceeds to controller
10. `@PreAuthorize` annotations enforce role requirements

#### 4. Token Refresh
1. Client sends `POST /api/v1/auth/refresh` with refresh token
2. `AuthService.refresh()` looks up token in database
3. Checks if token is expired
4. **Token Rotation**: Old refresh token deleted immediately
5. New access token generated
6. New refresh token generated and saved
7. Both new tokens returned to client

### Security Components Explained

#### JwtTokenProvider
Handles all JWT operations:
```java
// Generate access token with user claims
public String generateAccessToken(User user) {
    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
}

// Validate token signature and expiration
public boolean validateToken(String token) {
    Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token);
    return true;
}
```

#### JwtAuthenticationFilter
Spring Security filter that runs on every request:
```java
@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    // 1. Get JWT from Authorization header
    String jwt = getJwtFromRequest(request);
    
    // 2. Validate and authenticate if present
    if (hasText(jwt) && tokenProvider.validateToken(jwt)) {
        String email = tokenProvider.getEmailFromToken(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        // 3. Set authentication in security context
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    // 4. Continue filter chain
    filterChain.doFilter(request, response);
}
```

#### SecurityConfig
Defines which endpoints require authentication:
```java
.authorizeHttpRequests(auth -> auth
    // Public - no auth required
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/v1/products/**").permitAll()
    
    // Admin only
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    
    // All other API endpoints require authentication
    .requestMatchers("/api/v1/**").authenticated()
)
```

### Token Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           TOKEN LIFECYCLE                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  LOGIN                                                                  │
│    │                                                                    │
│    ▼                                                                    │
│  ┌─────────────────┐    ┌─────────────────┐                            │
│  │  Access Token   │    │  Refresh Token  │                            │
│  │  (15 minutes)   │    │    (7 days)     │                            │
│  └────────┬────────┘    └────────┬────────┘                            │
│           │                      │                                      │
│           ▼                      │                                      │
│  ┌─────────────────┐             │                                      │
│  │ Use for API     │             │                                      │
│  │ requests        │             │                                      │
│  └────────┬────────┘             │                                      │
│           │                      │                                      │
│           ▼                      │                                      │
│  ┌─────────────────┐             │                                      │
│  │ Token Expired?  │─── No ──────┼───▶ Continue using                   │
│  └────────┬────────┘             │                                      │
│           │ Yes                  │                                      │
│           ▼                      ▼                                      │
│  ┌─────────────────────────────────────┐                               │
│  │  POST /api/v1/auth/refresh          │                               │
│  │  Send refresh token                  │                               │
│  └─────────────────┬───────────────────┘                               │
│                    │                                                    │
│                    ▼                                                    │
│  ┌─────────────────────────────────────┐                               │
│  │  Old refresh token DELETED          │ ◀── Token Rotation            │
│  │  New access + refresh tokens issued │                               │
│  └─────────────────────────────────────┘                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Why Refresh Token Rotation?

Refresh token rotation enhances security by:

1. **Limiting Token Reuse**: Each refresh token can only be used once
2. **Detecting Theft**: If an attacker steals a refresh token and uses it, the legitimate user's next refresh attempt will fail (token already used/deleted), alerting them to a breach
3. **Reducing Attack Window**: Even if a token is compromised, it becomes invalid after first use

---

## Architecture

```
auth/
├── controller/          # REST endpoints
│   ├── AuthController.java
│   └── AdminUserController.java
├── dto/                 # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── RefreshRequest.java
│   ├── RegisterRequest.java
│   ├── TokenResponse.java
│   ├── UpdateRoleRequest.java
│   └── UserResponse.java
├── entity/              # JPA Entities
│   ├── RefreshToken.java
│   ├── Role.java
│   └── User.java
├── repository/          # Spring Data JPA Repositories
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
├── security/            # Security Configuration
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthEntryPoint.java
│   ├── JwtTokenProvider.java
│   └── SecurityConfig.java
└── service/             # Business Logic
    ├── AuthService.java
    └── AuthServiceImpl.java
```

---

## API Endpoints

### Public Endpoints (`/api/v1/auth`)

#### POST `/api/v1/auth/register`
Register a new user.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules:**
- `email`: Required, must be valid email format
- `password`: Required, minimum 6 characters
- `firstName`: Required
- `lastName`: Required

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2026-02-08T10:30:00"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid input
- `409 Conflict`: User already exists

---

#### POST `/api/v1/auth/login`
Authenticate user and obtain tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "base64-encoded-random-token",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid credentials

---

#### POST `/api/v1/auth/refresh`
Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "base64-encoded-refresh-token"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "new-base64-encoded-refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Notes:**
- Implements refresh token rotation (old token is invalidated)
- New refresh token is issued with each refresh

**Error Responses:**
- `401 Unauthorized`: Invalid or expired refresh token

---

#### GET `/api/v1/auth/me`
Get current authenticated user information.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2026-02-08T10:30:00"
}
```

**Error Responses:**
- `401 Unauthorized`: Missing or invalid token

---

### Admin Endpoints (`/api/v1/admin/users`)

> **Note:** All admin endpoints require `ADMIN` role.

#### GET `/api/v1/admin/users`
Get all users with pagination.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "USER",
      "createdAt": "2026-02-08T10:30:00"
    }
  ],
  "pageable": {...},
  "totalElements": 100,
  "totalPages": 5
}
```

---

#### GET `/api/v1/admin/users/{id}`
Get user by ID.

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2026-02-08T10:30:00"
}
```

**Error Responses:**
- `403 Forbidden`: Admin access required
- `404 Not Found`: User not found

---

#### PUT `/api/v1/admin/users/{id}/role`
Update user role.

**Request Body:**
```json
{
  "role": "ADMIN"
}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "ADMIN",
  "createdAt": "2026-02-08T10:30:00"
}
```

**Error Responses:**
- `403 Forbidden`: Admin access required
- `404 Not Found`: User not found

---

## Security Configuration

### JWT Token Structure

**Access Token Claims:**
| Claim | Description |
|-------|-------------|
| `sub` | User ID (UUID) |
| `email` | User's email address |
| `role` | User's role (USER/ADMIN) |
| `iat` | Issued at timestamp |
| `exp` | Expiration timestamp |

### Token Validity

| Token Type | Default Validity | Configuration Property |
|------------|------------------|----------------------|
| Access Token | 15 minutes (900,000 ms) | `app.jwt.access-token-validity-ms` |
| Refresh Token | 7 days (168 hours) | `app.jwt.refresh-token-validity-hours` |

### Password Encoding

- Algorithm: BCrypt
- Handled by Spring Security's `BCryptPasswordEncoder`

### Public Endpoints (No Auth Required)

- `/api/v1/auth/**` - All authentication endpoints
- `/api/v1/products/**` - Product catalog (read-only)
- `/swagger-ui/**` - Swagger UI
- `/v3/api-docs/**` - OpenAPI documentation
- `/actuator/health` - Health check endpoint

### Role-Based Access Control

| Role | Access Level |
|------|--------------|
| `USER` | Standard user access to authenticated endpoints |
| `ADMIN` | Full access including `/api/v1/admin/**` endpoints |

---

## Data Models

### User Entity

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key, auto-generated |
| `email` | String | Unique email address |
| `passwordHash` | String | BCrypt hashed password |
| `firstName` | String | User's first name |
| `lastName` | String | User's last name |
| `role` | Role | USER or ADMIN |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

### RefreshToken Entity

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key, auto-generated |
| `user` | User | Associated user (ManyToOne) |
| `token` | String | Unique refresh token string |
| `expiresAt` | LocalDateTime | Token expiration time |
| `createdAt` | LocalDateTime | Creation timestamp |

### Role Enum

```java
public enum Role {
    USER,
    ADMIN
}
```

---

## Configuration Properties

Add these to `application.yml`:

```yaml
app:
  jwt:
    secret: your-256-bit-secret-key-for-jwt-signing
    access-token-validity-ms: 900000    # 15 minutes
    refresh-token-validity-hours: 168   # 7 days
```

> **Important:** The JWT secret must be at least 256 bits (32 characters) for HS256 algorithm.

---

## Security Features

### 1. Stateless Authentication
- No server-side sessions
- JWT tokens contain all necessary user information
- Horizontal scaling friendly

### 2. Refresh Token Rotation
- Old refresh tokens are invalidated upon use
- Limits damage from token theft
- Each refresh generates a new refresh token

### 3. Password Security
- Passwords stored using BCrypt hashing
- Never exposed in responses
- Minimum 6 character requirement

### 4. Method-Level Security
- `@PreAuthorize` annotations for fine-grained access control
- Role-based endpoint protection

---

## Error Handling

The module uses standard RFC 7807 ProblemDetail responses through the common exception handlers:

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ConflictException` | 409 | Duplicate email registration |
| `UnauthorizedException` | 401 | Invalid credentials, expired tokens |
| `ResourceNotFoundException` | 404 | User not found |

---

## Usage Examples

### Register and Login Flow

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'

# 2. Login to get tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'

# 3. Use access token for authenticated requests
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <access_token>"

# 4. Refresh token when access token expires
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh_token>"
  }'
```

---

## Database Schema

### users table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

### refresh_tokens table
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## Related Documentation

- [OrderHub MVP Specification](OrderHub_MVP.md)
- [Product Requirements Document](prd.md)
- [Implementation Plans](plans/06-auth-backend.md)
