# Feature 02: Docker Infrastructure

**Priority**: Foundation
**Dependencies**: None
**Parallel with**: 01-backend-scaffolding, 03-angular-scaffolding
**Blocks**: 04-database-schema

---

## Overview

Set up Docker Compose for local development (PostgreSQL database) and a Dockerfile for the Spring Boot backend. This enables the database to be running before any backend code is tested.

## User Stories

- US-027 (partial): Health check — Docker Compose enables the infrastructure for this

## Tasks

### 2.1 Docker Compose

- [x] Create `docker-compose.yml` at project root with:
  - `db` service:
    - Image: `postgres:16-alpine`
    - Ports: `5432:5432`
    - Environment: `POSTGRES_DB=orderhub`, `POSTGRES_USER=orderhub`, `POSTGRES_PASSWORD=orderhub`
    - Volume: `pgdata:/var/lib/postgresql/data`
    - Health check: `pg_isready -U orderhub` (interval 5s, timeout 5s, retries 5)
  - `app` service:
    - Build context: `./backend`
    - Ports: `8080:8080`
    - Environment: datasource URL, username, password, `SPRING_PROFILES_ACTIVE=dev`
    - Depends on: `db` with `condition: service_healthy`
  - Volume: `pgdata`

### 2.2 Backend Dockerfile

- [x] Create `backend/Dockerfile` with multi-stage build:
  - **Stage 1 (build)**: `maven:3.9-eclipse-temurin-17` base
    - Copy `pom.xml`, download dependencies
    - Copy `src/`, run `mvn clean package -DskipTests`
  - **Stage 2 (runtime)**: `eclipse-temurin:17-jre` base
    - Copy JAR from build stage
    - Expose port 8080
    - `ENTRYPOINT ["java", "-jar", "app.jar"]`

### 2.3 Docker ignore

- [x] Create `backend/.dockerignore`:
  - `target/`
  - `.git`
  - `*.md`
  - `.idea/`

## Verification

- [x] `docker compose up db -d` starts PostgreSQL and health check passes
- [x] Can connect to database: `psql -h localhost -U orderhub -d orderhub`
- [x] `docker compose down` shuts down cleanly
- [ ] (After Feature 01 is complete) `docker compose up --build` builds and starts the app service

## Files Created

```
docker-compose.yml
backend/
├── Dockerfile
└── .dockerignore
```
