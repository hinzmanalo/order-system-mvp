# Deployment Guide

Production deployment guide for OrderHub backend.

## Table of Contents

- [Environment Setup](#environment-setup)
- [Building for Production](#building-for-production)
- [Docker Deployment](#docker-deployment)
- [Database Setup](#database-setup)
- [Environment Variables](#environment-variables)
- [Health Checks](#health-checks)
- [Monitoring & Logging](#monitoring--logging)
- [Backup & Recovery](#backup--recovery)
- [Scaling](#scaling)

---

## Environment Setup

### System Requirements

**Minimum:**
- CPU: 2 cores
- RAM: 2 GB
- Disk: 10 GB
- OS: Linux, macOS, or Windows with WSL2

**Recommended (Production):**
- CPU: 4+ cores
- RAM: 4+ GB
- Disk: 50+ GB SSD
- OS: Ubuntu 22.04 LTS or similar

### Software Dependencies

```bash
# Java 17
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
# openjdk version "17.0.x"

# PostgreSQL 16
sudo apt install postgresql-16 postgresql-contrib

# Docker (optional but recommended)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

---

## Building for Production

### Maven Build

```bash
# Navigate to backend directory
cd backend

# Run tests
mvn clean test

# Build JAR (skip tests if already run)
mvn clean package -DskipTests

# JAR created at:
# target/orderhub-0.0.1-SNAPSHOT.jar
```

### Optimized Build

```bash
# Build with production profile
mvn clean package -Pprod -DskipTests

# Create executable JAR with dependencies
mvn clean package spring-boot:repackage
```

### Verify Build

```bash
# Check JAR integrity
jar -tf target/orderhub-0.0.1-SNAPSHOT.jar | head -20

# Test run
java -jar target/orderhub-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

---

## Docker Deployment

### Build Docker Image

**Dockerfile** (already provided):
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build image:**
```bash
# From project root
docker build -t orderhub-backend:latest ./backend

# Tag for registry (optional)
docker tag orderhub-backend:latest registry.example.com/orderhub-backend:v1.0.0
```

### Run with Docker Compose

**Production docker-compose.yml:**
```yaml
version: '3.8'

services:
  db:
    image: postgres:16-alpine
    container_name: orderhub-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: orderhub
      POSTGRES_USER: orderhub
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U orderhub"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    image: orderhub-backend:latest
    container_name: orderhub-backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/orderhub
      SPRING_DATASOURCE_USERNAME: orderhub
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  postgres_data:
```

**Start services:**
```bash
# Set environment variables
export DB_PASSWORD="secure-db-password-here"
export JWT_SECRET="production-jwt-secret-at-least-256-bits"

# Start services
docker compose up -d

# Check logs
docker compose logs -f backend

# Check status
docker compose ps
```

### Deploy to Cloud

#### AWS ECS

```bash
# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin {account-id}.dkr.ecr.us-east-1.amazonaws.com

docker tag orderhub-backend:latest {account-id}.dkr.ecr.us-east-1.amazonaws.com/orderhub-backend:latest

docker push {account-id}.dkr.ecr.us-east-1.amazonaws.com/orderhub-backend:latest
```

#### Google Cloud Run

```bash
# Build and deploy
gcloud builds submit --tag gcr.io/{project-id}/orderhub-backend

gcloud run deploy orderhub-backend \
  --image gcr.io/{project-id}/orderhub-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod,JWT_SECRET=${JWT_SECRET}"
```

#### Azure Container Instances

```bash
az container create \
  --resource-group orderhub-rg \
  --name orderhub-backend \
  --image orderhub-backend:latest \
  --dns-name-label orderhub \
  --ports 8080 \
  --environment-variables \
    SPRING_PROFILES_ACTIVE=prod \
    JWT_SECRET=${JWT_SECRET}
```

---

## Database Setup

### Production Database Creation

```bash
# Connect to PostgreSQL
sudo -u postgres psql

# Create database and user
CREATE DATABASE orderhub;
CREATE USER orderhub_app WITH ENCRYPTED PASSWORD 'secure-password-here';
GRANT ALL PRIVILEGES ON DATABASE orderhub TO orderhub_app;

# Connect to orderhub database
\c orderhub

# Grant schema permissions
GRANT ALL ON SCHEMA public TO orderhub_app;
```

### Run Migrations

Migrations run automatically on application startup via Flyway.

**Manual migration (if needed):**
```bash
# Using Maven
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/orderhub \
  -Dflyway.user=orderhub_app \
  -Dflyway.password=secure-password

# Check migration history
mvn flyway:info
```

### Database Tuning

**postgresql.conf optimizations:**
```conf
# Connection settings
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
work_mem = 4MB

# Write-ahead log
wal_buffers = 16MB
min_wal_size = 1GB
max_wal_size = 4GB

# Query planning
random_page_cost = 1.1  # SSD
effective_io_concurrency = 200

# Logging
log_min_duration_statement = 1000  # Log slow queries (>1s)
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
```

---

## Environment Variables

### Required Variables

| Variable | Example | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/orderhub` | Database connection URL |
| `SPRING_DATASOURCE_USERNAME` | `orderhub_app` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `***` | Database password |
| `JWT_SECRET` | `***` | JWT signing secret (min 256 bits) |

### Optional Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (dev, test, prod) |
| `SERVER_PORT` | `8080` | Application port |
| `APP_JWT_ACCESS_TOKEN_VALIDITY_MS` | `900000` | Access token lifetime (15 min) |
| `APP_JWT_REFRESH_TOKEN_VALIDITY_HOURS` | `168` | Refresh token lifetime (7 days) |

### Production Configuration File

**application-prod.yml:**
```yaml
server:
  port: 8080
  shutdown: graceful
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  flyway:
    enabled: true
    validate-on-migrate: true

logging:
  level:
    root: INFO
    com.orderhub: INFO
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} - %msg%n'
  file:
    name: /var/log/orderhub/application.log
    max-size: 10MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-validity-ms: ${APP_JWT_ACCESS_TOKEN_VALIDITY_MS:900000}
    refresh-token-validity-hours: ${APP_JWT_REFRESH_TOKEN_VALIDITY_HOURS:168}
```

---

## Health Checks

### Application Health Endpoint

```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Response (healthy)
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Database Health

```bash
# PostgreSQL connection check
docker exec orderhub-db pg_isready -U orderhub

# Query health
docker exec orderhub-db psql -U orderhub -c "SELECT 1;"
```

### Load Balancer Health

Configure load balancer to poll:
```
GET /actuator/health
Expected: 200 OK
Interval: 30s
Timeout: 5s
Unhealthy threshold: 3
```

---

## Monitoring & Logging

### Application Metrics

**Prometheus metrics:**
```bash
# Metrics endpoint
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**Prometheus scrape config:**
```yaml
scrape_configs:
  - job_name: 'orderhub-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

### Centralized Logging

**Send logs to ELK Stack:**

```yaml
# logback-spring.xml
<configuration>
  <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
  </appender>
  
  <root level="INFO">
    <appender-ref ref="LOGSTASH" />
  </root>
</configuration>
```

### Alert Rules

**Prometheus alert rules:**
```yaml
groups:
  - name: orderhub_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 10m
        annotations:
          summary: "Memory usage above 90%"
      
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active >= hikaricp_connections_max
        for: 2m
        annotations:
          summary: "Database connection pool exhausted"
```

---

## Backup & Recovery

### Database Backup

**Automated backup script:**
```bash
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="orderhub_backup_${TIMESTAMP}.sql"

# Create backup
docker exec orderhub-db pg_dump -U orderhub orderhub > "$BACKUP_DIR/$BACKUP_FILE"

# Compress
gzip "$BACKUP_DIR/$BACKUP_FILE"

# Delete backups older than 30 days
find "$BACKUP_DIR" -name "orderhub_backup_*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

**Schedule with cron:**
```bash
# Daily backup at 2 AM
0 2 * * * /path/to/backup-db.sh >> /var/log/orderhub-backup.log 2>&1
```

### Restore from Backup

```bash
# Stop application
docker compose stop backend

# Restore database
gunzip -c orderhub_backup_20260213_020000.sql.gz | \
  docker exec -i orderhub-db psql -U orderhub orderhub

# Restart application
docker compose start backend
```

### Disaster Recovery

**Full system restore:**
1. Deploy fresh infrastructure
2. Restore database from latest backup
3. Update environment variables
4. Deploy latest application version
5. Run health checks
6. Update DNS/load balancer

**Recovery Time Objective (RTO):** < 1 hour  
**Recovery Point Objective (RPO):** < 24 hours (daily backups)

---

## Scaling

### Horizontal Scaling

**Load balancer (Nginx):**
```nginx
upstream orderhub_backend {
    least_conn;
    server backend1:8080 max_fails=3 fail_timeout=30s;
    server backend2:8080 max_fails=3 fail_timeout=30s;
    server backend3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    location / {
        proxy_pass http://orderhub_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**Docker Compose scale:**
```bash
# Scale to 3 instances
docker compose up -d --scale backend=3
```

### Database Scaling

**Read replicas:**
```yaml
# application-prod.yml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary-db:5432/orderhub
      username: orderhub_app
    replica:
      url: jdbc:postgresql://replica-db:5432/orderhub
      username: orderhub_readonly
```

**Connection pooling:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### Performance Tuning

**JVM options:**
```bash
java -jar \
  -Xms512m \
  -Xmx2048m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/orderhub/heap_dump.hprof \
  orderhub.jar
```

**Application properties:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 20
        order_inserts: true
        query.in_clause_parameter_padding: true
```

---

## Security Checklist

- [ ] Change default JWT secret to strong random value
- [ ] Use environment variables for all secrets
- [ ] Enable HTTPS/TLS in production
- [ ] Configure rate limiting on endpoints
- [ ] Restrict database user permissions
- [ ] Enable database SSL connections
- [ ] Set up Web Application Firewall (WAF)
- [ ] Regular security updates for dependencies
- [ ] Enable audit logging
- [ ] Implement IP whitelisting for admin endpoints

---

## Troubleshooting Production Issues

### High CPU Usage

```bash
# Check Java threads
docker exec orderhub-backend jstack 1 > thread_dump.txt

# Monitor with top
docker exec orderhub-backend top
```

### Memory Leak

```bash
# Generate heap dump
docker exec orderhub-backend jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# Copy to host
docker cp orderhub-backend:/tmp/heapdump.hprof ./

# Analyze with VisualVM or Eclipse MAT
```

### Database Connection Pool Exhausted

```bash
# Check active connections
docker exec orderhub-db psql -U orderhub -c \
  "SELECT count(*) FROM pg_stat_activity WHERE datname='orderhub';"

# Increase pool size in application-prod.yml
spring.datasource.hikari.maximum-pool-size: 30
```

### Slow Queries

```bash
# Enable query logging in PostgreSQL
docker exec orderhub-db psql -U postgres -c \
  "ALTER SYSTEM SET log_min_duration_statement = 1000;"

# Reload config
docker exec orderhub-db psql -U postgres -c "SELECT pg_reload_conf();"

# View logs
docker logs orderhub-db | grep "duration:"
```

---

**Production Deployment Complete!** Monitor [health endpoints](http://localhost:8080/actuator/health) and check logs regularly 😊
