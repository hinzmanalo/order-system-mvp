# Phase 18: Disable Security/JWT During Local Development

## Goal

Allow developers to run the full stack locally without configuring or dealing with JWT authentication. All API endpoints become accessible without tokens, and the frontend skips auth guards and token attachment — all toggled via a Spring profile (`nosecurity`) and an Angular environment flag (`authBypass`).

## Approach

Use Spring's `@Profile` annotation to swap [`SecurityConfig`](../../backend/src/main/java/com/orderhub/auth/security/SecurityConfig.java) with a permissive alternative when the `nosecurity` profile is active. A dev filter auto-injects a mock ADMIN user into the `SecurityContext` so `@PreAuthorize` annotations (used on 4 admin controllers) continue to work without errors.

On the frontend, an `authBypass` flag in the development environment config makes guards pass-through and the interceptor skip token logic.

**Production is never affected** — the `nosecurity` profile is opt-in and doesn't exist in production builds.

---

## Backend Changes (4 files)

### 1. Exclude existing `SecurityConfig` when `nosecurity` profile is active

**File**: [SecurityConfig.java](../../backend/src/main/java/com/orderhub/auth/security/SecurityConfig.java)

Add `@Profile("!nosecurity")` to the class so it is **not loaded** when the `nosecurity` profile is active:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!nosecurity")        // ← ADD THIS
public class SecurityConfig {
    // ... existing code unchanged
}
```

### 2. Create `DevSecurityConfig` — permits all requests

**New file**: `backend/src/main/java/com/orderhub/auth/security/DevSecurityConfig.java`

Only loaded when `nosecurity` profile is active. Permits all HTTP requests with no JWT filter:

```java
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("nosecurity")
public class DevSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(DevSecurityConfig.class);

    private final DevAuthenticationFilter devAuthenticationFilter;

    public DevSecurityConfig(DevAuthenticationFilter devAuthenticationFilter) {
        this.devAuthenticationFilter = devAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.warn("⚠️  SECURITY DISABLED — nosecurity profile is active. DO NOT use in production!");

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3. Create `DevAuthenticationFilter` — auto-injects mock ADMIN user

**New file**: `backend/src/main/java/com/orderhub/auth/security/DevAuthenticationFilter.java`

Sets a mock `ADMIN` user in the `SecurityContext` for every request so that `@PreAuthorize("hasRole('ADMIN')")` on the 4 admin controllers resolves successfully:

```java
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

@Component
@Profile("nosecurity")
public class DevAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DevAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            );

            var auth = new UsernamePasswordAuthenticationToken(
                "dev-admin@orderhub.local",  // principal
                null,                         // credentials
                authorities
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            logger.debug("Dev auth filter: injected mock ADMIN user for {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
```

### 4. Create `application-nosecurity.yml`

**New file**: `backend/src/main/resources/application-nosecurity.yml`

Minimal config — the profile itself is what triggers the bean swap. This file documents intent and can hold any future dev-only overrides:

```yaml
# NoSecurity Profile
# Disables JWT authentication — all endpoints are accessible without tokens.
# A mock ADMIN user is injected into every request's SecurityContext.
#
# Usage: mvn spring-boot:run -Dspring-boot.run.profiles=dev,nosecurity
#
# WARNING: Never activate this profile in production!

logging:
  level:
    com.orderhub.auth.security: DEBUG
```

---

## Frontend Changes (3 files)

### 5. Add `authBypass` flag to environment files

**File**: `frontend/src/environments/environment.development.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: "/api/v1",
  authBypass: true, // ← ADD — skips guards and token interceptor
};
```

**File**: `frontend/src/environments/environment.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: "/api/v1",
  authBypass: false, // ← ADD — always false in production
};
```

### 6. Update `authGuard` to bypass when flag is set

**File**: `frontend/src/app/core/guards/auth.guard.ts`

```typescript
import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";
import { environment } from "../../../environments/environment"; // ← ADD

export const authGuard: CanActivateFn = (route, state) => {
  if (environment.authBypass) return true; // ← ADD

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(["/login"], { queryParams: { returnUrl: state.url } });
  return false;
};
```

### 7. Update `adminGuard` to bypass when flag is set

**File**: `frontend/src/app/core/guards/admin.guard.ts`

```typescript
import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";
import { environment } from "../../../environments/environment"; // ← ADD

export const adminGuard: CanActivateFn = (route, state) => {
  if (environment.authBypass) return true; // ← ADD

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  router.navigate(["/products"]);
  return false;
};
```

### 8. Update `authInterceptor` to skip token logic when flag is set

**File**: `frontend/src/app/core/interceptors/auth.interceptor.ts`

Add an early return at the top of the interceptor:

```typescript
import { environment } from "../../../environments/environment"; // ← ADD

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (environment.authBypass) return next(req); // ← ADD — skip all token logic

  const authService = inject(AuthService);
  // ... rest unchanged
};
```

---

## How to Use

### Backend — run without security

```bash
# Option 1: Maven command line
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev,nosecurity

# Option 2: Environment variable
export SPRING_PROFILES_ACTIVE=dev,nosecurity
mvn spring-boot:run

# Option 3: IntelliJ / VS Code launch config
# Set active profiles to: dev,nosecurity
```

### Frontend — auth bypass is automatic in dev

```bash
cd frontend
ng serve    # uses environment.development.ts where authBypass=true
```

### Full stack (Docker) — if desired

Add to `docker-compose.yml` as an override:

```yaml
services:
  backend:
    environment:
      - SPRING_PROFILES_ACTIVE=dev,nosecurity
```

### Restore security

- **Backend**: Remove `nosecurity` from profiles → `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- **Frontend**: Set `authBypass: false` in `environment.development.ts` (or it's already `false` in production builds)

---

## Files Changed Summary

| #   | File                                                     | Action     | Description                                |
| --- | -------------------------------------------------------- | ---------- | ------------------------------------------ |
| 1   | `backend/.../security/SecurityConfig.java`               | **Modify** | Add `@Profile("!nosecurity")`              |
| 2   | `backend/.../security/DevSecurityConfig.java`            | **Create** | Permit-all security config                 |
| 3   | `backend/.../security/DevAuthenticationFilter.java`      | **Create** | Auto-inject mock ADMIN user                |
| 4   | `backend/src/main/resources/application-nosecurity.yml`  | **Create** | Profile config with debug logging          |
| 5   | `frontend/src/environments/environment.development.ts`   | **Modify** | Add `authBypass: true`                     |
| 6   | `frontend/src/environments/environment.ts`               | **Modify** | Add `authBypass: false`                    |
| 7   | `frontend/src/app/core/guards/auth.guard.ts`             | **Modify** | Bypass when `authBypass` is true           |
| 8   | `frontend/src/app/core/guards/admin.guard.ts`            | **Modify** | Bypass when `authBypass` is true           |
| 9   | `frontend/src/app/core/interceptors/auth.interceptor.ts` | **Modify** | Skip token logic when `authBypass` is true |

---

## Verification Checklist

- [ ] Backend starts with `dev,nosecurity` profiles without errors
- [ ] `GET /api/v1/orders` returns 200 without JWT token
- [ ] `POST /api/v1/admin/products` returns 200 without JWT token (mock ADMIN user)
- [ ] Swagger UI works at `/swagger-ui.html`
- [ ] Frontend `ng serve` — can navigate to `/admin/products` without logging in
- [ ] Frontend `ng serve` — API calls succeed without token attachment
- [ ] Backend starts with only `dev` profile — security is fully enforced (no regression)
- [ ] `ng build --configuration production` — `authBypass` is `false`, guards enforce auth

---

## Risk Assessment

| Risk                                              | Mitigation                                                                                                       |
| ------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `nosecurity` accidentally enabled in production   | Profile name is explicit; production deploys don't include it; `application-nosecurity.yml` has WARNING comments |
| Mock user principal doesn't match real user shape | For most dev work this is fine; when testing auth-specific features, switch back to the secured profile          |
| Tests that depend on security behavior break      | Unit tests don't activate profiles by default; the `nosecurity` profile is opt-in at runtime only                |
| Frontend build includes bypass in production      | `environment.ts` (production) has `authBypass: false`; Angular's file replacement ensures correct file is used   |
