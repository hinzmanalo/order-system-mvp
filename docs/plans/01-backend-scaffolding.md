# Feature 01: Backend Scaffolding

**Priority**: Foundation
**Dependencies**: None
**Parallel with**: 02-docker-infrastructure, 03-angular-scaffolding
**Blocks**: 04-database-schema, 05-common-module

---

## Overview

Set up the Spring Boot 3 Maven project structure with all required dependencies, the main application class, and configuration files.

## User Stories

- N/A (infrastructure)

## Tasks

### 1.1 Maven project (`backend/pom.xml`)

- [ ] Create `backend/` directory
- [ ] Create `pom.xml` with Spring Boot 3.x parent
- [ ] Add compile dependencies:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-actuator`
  - `postgresql` driver
  - `flyway-core` + `flyway-database-postgresql`
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (io.jsonwebtoken)
  - `springdoc-openapi-starter-webmvc-ui` 2.x
  - `lombok` (optional scope)
- [ ] Add test dependencies:
  - `spring-boot-starter-test`
  - `spring-security-test`
  - `testcontainers` BOM + `postgresql` module
  - `assertj-core`
- [ ] Configure Maven plugins:
  - `spring-boot-maven-plugin`
  - `maven-surefire-plugin` (unit tests)
  - `maven-failsafe-plugin` (integration tests)

### 1.2 Main application class

- [ ] Create directory structure: `backend/src/main/java/com/orderhub/`
- [ ] Create `OrderHubApplication.java` with `@SpringBootApplication` and `main()` method

### 1.3 Application configuration

- [ ] Create `backend/src/main/resources/application.yml`:
  - `server.port: 8080`
  - `spring.jpa.hibernate.ddl-auto: validate`
  - `spring.jpa.show-sql: false`
  - `spring.jpa.properties.hibernate.format_sql: true`
  - `spring.flyway.enabled: true`
  - `management.endpoints.web.exposure.include: health,info`
  - `app.jwt.secret: ${JWT_SECRET:default-dev-secret-key-that-is-at-least-256-bits-long}`
  - `app.jwt.access-token-expiry: 900000` (15 min in ms)
  - `app.jwt.refresh-token-expiry: 604800000` (7 days in ms)
- [ ] Create `backend/src/main/resources/application-dev.yml`:
  - `spring.datasource.url: jdbc:postgresql://localhost:5432/orderhub`
  - `spring.datasource.username: orderhub`
  - `spring.datasource.password: orderhub`
  - `spring.jpa.show-sql: true`

### 1.4 Git ignores

- [ ] Update root `.gitignore` to include:
  - `backend/target/`
  - `*.class`
  - `.idea/`
  - `*.iml`

### 1.5 Test structure

- [ ] Create `backend/src/test/java/com/orderhub/` directory
- [ ] Create placeholder `OrderHubApplicationTests.java`

## Verification

- [ ] `cd backend && mvn clean compile` succeeds without errors
- [ ] Project structure matches: `backend/src/main/java/com/orderhub/OrderHubApplication.java`
- [ ] `application.yml` and `application-dev.yml` are present under `src/main/resources/`

## Files Created

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/orderhub/
    │   │   └── OrderHubApplication.java
    │   └── resources/
    │       ├── application.yml
    │       └── application-dev.yml
    └── test/
        └── java/com/orderhub/
            └── OrderHubApplicationTests.java
```
