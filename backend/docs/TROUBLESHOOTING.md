# Troubleshooting Guide

Common issues and solutions for OrderHub backend.

## Table of Contents

- [Application Issues](#application-issues)
- [Database Issues](#database-issues)
- [Authentication Issues](#authentication-issues)
- [API Errors](#api-errors)
- [Performance Issues](#performance-issues)
- [Docker Issues](#docker-issues)
- [Development Issues](#development-issues)

---

## Application Issues

### Application Won't Start

#### Error: "Port 8080 is already in use"

**Symptoms:**

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Web server failed to start. Port 8080 was already in use.
```

**Cause:** Another process is using port 8080

**Solutions:**

```bash
# 1. Find process using port
lsof -i :8080

# 2. Kill the process
kill -9 {PID}

# 3. Or change application port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# 4. Or in docker-compose.yml
services:
  backend:
    ports:
      - "8081:8080"
```

#### Error: "Failed to configure a DataSource"

**Symptoms:**

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 'url' attribute is not specified
```

**Cause:** Database connection not configured or PostgreSQL not running

**Solutions:**

```bash
# 1. Check if database is running
docker compose ps

# 2. Start database
docker compose up db -d

# 3. Verify connection
psql -h localhost -U orderhub -d orderhub
# Password: orderhub

# 4. Check application.yml database config
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderhub
    username: orderhub
    password: orderhub
```

#### Error: Application starts but crashes immediately

**Symptoms:**

```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

**Cause:** Insufficient JVM memory

**Solutions:**

```bash
# Increase heap size
java -jar -Xms512m -Xmx2048m orderhub.jar

# Or in Maven
export MAVEN_OPTS="-Xmx2048m"
mvn spring-boot:run

# In Docker, set memory limits
docker compose up --scale backend=1 --memory="2g"
```

---

## Database Issues

### Migration Checksum Mismatch

**Symptoms:**

```
FlywayException: Validate failed:
Migration checksum mismatch for migration version V1
Expected: 123456789
Actual:   987654321
```

**Cause:** Existing migration file was modified after being applied

**Solutions:**

**Development (Reset database):**

```bash
# Stop containers
docker compose down

# Remove volumes (WARNING: Deletes all data)
docker compose down -v

# Restart
docker compose up -d
```

**Production (Repair):**

```bash
# Repair checksums
mvn flyway:repair

# Or manually in database
UPDATE flyway_schema_history
SET checksum = {actual-checksum}
WHERE version = 'V1';
```

### Cannot connect to PostgreSQL

**Symptoms:**

```
org.postgresql.util.PSQLException: Connection refused
```

**Cause:** PostgreSQL not running or wrong connection details

**Solutions:**

```bash
# 1. Check if PostgreSQL is running
docker compose ps db

# 2. Check PostgreSQL logs
docker compose logs db

# 3. Verify connection manually
psql -h localhost -p 5432 -U orderhub -d orderhub

# 4. Check if port is exposed
docker compose port db 5432

# 5. Restart PostgreSQL
docker compose restart db
```

### Too Many Database Connections

**Symptoms:**

```
PSQLException: FATAL: sorry, too many clients already
```

**Cause:** Connection pool exhausted or connections not being closed

**Solutions:**

```bash
# 1. Check active connections
docker exec orderhub-db psql -U orderhub -c \
  "SELECT count(*) FROM pg_stat_activity WHERE datname='orderhub';"

# 2. Increase max connections in PostgreSQL
docker exec orderhub-db psql -U postgres -c \
  "ALTER SYSTEM SET max_connections = 200;"

# 3. Restart PostgreSQL
docker compose restart db

# 4. Adjust connection pool in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Slow Queries

**Symptoms:**

- API responses taking several seconds
- Database CPU usage high

**Solutions:**

```bash
# 1. Enable query logging
docker exec orderhub-db psql -U postgres -c \
  "ALTER SYSTEM SET log_min_duration_statement = 1000;"

# 2. Reload config
docker exec orderhub-db psql -U postgres -c \
  "SELECT pg_reload_conf();"

# 3. Check logs for slow queries
docker logs orderhub-db 2>&1 | grep "duration:"

# 4. Create missing indexes
# Example: Add index on frequently queried column
CREATE INDEX idx_orders_user_id_created_at
ON orders(user_id, created_at DESC);

# 5. Analyze query performance
EXPLAIN ANALYZE SELECT * FROM orders WHERE user_id = 'uuid';
```

---

## Authentication Issues

### JWT Token Expired

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "JWT token has expired"
}
```

**Cause:** Access token validity is 15 minutes

**Solution:**

```bash
# Use refresh token to get new access token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'

# Response will include new access and refresh tokens
```

### Invalid JWT Signature

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid JWT signature"
}
```

**Cause:** JWT_SECRET mismatch or token tampered with

**Solutions:**

```bash
# 1. Check JWT_SECRET is consistent
echo $JWT_SECRET

# 2. In docker-compose.yml, ensure same secret
environment:
  JWT_SECRET: same-secret-in-all-instances

# 3. Login again to get fresh token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password"
  }'
```

### 403 Forbidden

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access denied"
}
```

**Cause:** User doesn't have required role (e.g., trying to access admin endpoint as regular user)

**Solution:**

```bash
# Check user role
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer {token}"

# Response shows role
{
  "id": "...",
  "email": "user@example.com",
  "role": "ROLE_USER"  # Need ROLE_ADMIN for admin endpoints
}

# Admin must grant ROLE_ADMIN (admin-only operation)
curl -X PUT http://localhost:8080/api/v1/admin/users/{userId}/role \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{"role": "ROLE_ADMIN"}'
```

### Password Doesn't Match

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid credentials"
}
```

**Cause:** Wrong password or user doesn't exist

**Solutions:**

```bash
# 1. Verify email is correct
# 2. Check password requirements (min 8 chars)
# 3. Use forgotten password flow (if implemented)

# For development, check user in database
docker exec orderhub-db psql -U orderhub orderhub -c \
  "SELECT email, role FROM users WHERE email = 'user@example.com';"
```

---

## API Errors

### 400 Bad Request - Validation Error

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation errors occurred",
  "errors": {
    "price": "Price must be positive",
    "name": "Product name is required"
  }
}
```

**Cause:** Request body doesn't meet validation constraints

**Solution:**

```bash
# Check API documentation for required fields
# Example: Creating product requires all fields

curl -X POST http://localhost:8080/api/v1/admin/products \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Product Name",        # Required, not blank
    "description": "Description",  # Optional
    "price": 99.99,                # Required, positive
    "sku": "SKU-001",              # Required, not blank
    "initialStock": 100            # Positive number
  }'
```

### 404 Not Found

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Product with ID {id} does not exist"
}
```

**Cause:** Resource doesn't exist or ID is incorrect

**Solutions:**

```bash
# 1. Verify resource ID is correct (must be valid UUID)
# Bad: "123"
# Good: "123e4567-e89b-12d3-a456-426614174000"

# 2. List resources to find correct ID
curl http://localhost:8080/api/v1/products

# 3. Check if resource was deleted
```

### 409 Conflict - Optimistic Lock Exception

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/optimistic-lock",
  "title": "Resource Modified",
  "status": 409,
  "detail": "Inventory was modified by another transaction. Please retry."
}
```

**Cause:** Resource was updated by another request between your read and write

**Solution:**

```bash
# 1. Get fresh data
curl http://localhost:8080/api/v1/admin/inventory/{productId} \
  -H "Authorization: Bearer {token}"

# Response:
# {
#   "productId": "...",
#   "quantity": 100,
#   "version": 6  # Use this version
# }

# 2. Retry update with new version
curl -X PUT http://localhost:8080/api/v1/admin/inventory/{productId} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 150,
    "version": 6  # Version from GET response
  }'
```

### 409 Conflict - Insufficient Stock

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/insufficient-stock",
  "title": "Insufficient Stock",
  "status": 409,
  "detail": "Product 'Headphones' has only 5 units available, but 10 were requested"
}
```

**Cause:** Order quantity exceeds available inventory

**Solutions:**

```bash
# 1. Check current stock
curl http://localhost:8080/api/v1/products/{productId}

# 2. Reduce order quantity
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "...",
        "quantity": 5  # Reduced to available stock
      }
    ]
  }'

# 3. Admin can increase inventory
curl -X PUT http://localhost:8080/api/v1/admin/inventory/{productId} \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 100,
    "version": {current-version}
  }'
```

### 500 Internal Server Error

**Symptoms:**

```json
{
  "type": "https://orderhub.com/errors/internal",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred"
}
```

**Cause:** Unexpected application error

**Solutions:**

```bash
# 1. Check application logs
docker compose logs backend | tail -50

# 2. Look for stack traces
docker compose logs backend 2>&1 | grep -A 20 "Exception"

# 3. Enable debug logging (application.yml)
logging:
  level:
    com.orderhub: DEBUG

# 4. Restart application
docker compose restart backend

# 5. If persistent, file bug report with:
#    - Request that caused error
#    - Stack trace from logs
#    - Steps to reproduce
```

---

## Performance Issues

### Slow API Responses

**Symptoms:**

- Requests taking multiple seconds
- Timeouts

**Diagnosis:**

```bash
# 1. Check application metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# 2. Enable SQL logging (application.yml)
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# 3. Check for N+1 queries in logs
```

**Solutions:**

```bash
# 1. Add database indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

# 2. Enable query batching
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 20
        order_inserts: true

# 3. Use pagination for large datasets
GET /api/v1/products?size=20&page=0

# 4. Enable HTTP compression
server:
  compression:
    enabled: true
```

### High Memory Usage

**Symptoms:**

```
java.lang.OutOfMemoryError: Java heap space
```

**Diagnosis:**

```bash
# 1. Check memory usage
docker stats orderhub-backend

# 2. Generate heap dump
docker exec orderhub-backend jcmd 1 GC.heap_dump /tmp/heap.hprof

# 3. Copy to host for analysis
docker cp orderhub-backend:/tmp/heap.hprof ./
```

**Solutions:**

```bash
# 1. Increase heap size
java -jar -Xms1g -Xmx2g orderhub.jar

# 2. In Docker Compose
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 2G

# 3. Review code for memory leaks
# - Unclosed resources
# - Large collections in memory
# - Caching issues

# 4. Use pagination to limit result sets
```

### High CPU Usage

**Symptoms:**

- CPU constantly at 100%
- Application unresponsive

**Diagnosis:**

```bash
# 1. Check CPU usage
docker stats orderhub-backend

# 2. Generate thread dump
docker exec orderhub-backend jstack 1 > threads.txt

# 3. Look for busy threads
grep "RUNNABLE" threads.txt -A 2
```

**Solutions:**

```bash
# 1. Check for infinite loops in code
# 2. Review database query performance
# 3. Add caching for frequently accessed data
# 4. Scale horizontally if needed

# Enable G1 garbage collector (better for large heaps)
java -jar -XX:+UseG1GC -XX:MaxGCPauseMillis=200 orderhub.jar
```

---

## Docker Issues

### Docker Compose Won't Start

**Symptoms:**

```
ERROR: Version in "./docker-compose.yml" is unsupported
```

**Solution:**

```bash
# Update Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify version
docker compose version
```

### Container Keeps Restarting

**Symptoms:**

```bash
$ docker compose ps
NAME                STATUS
orderhub-backend    Restarting (1) 5 seconds ago
```

**Diagnosis:**

```bash
# Check logs
docker compose logs backend

# Check exit code
docker inspect orderhub-backend --format='{{.State.ExitCode}}'
```

**Solutions:**

```bash
# 1. Fix configuration errors in docker-compose.yml
# 2. Ensure database is healthy before backend starts
services:
  backend:
    depends_on:
      db:
        condition: service_healthy

# 3. Check environment variables are set
docker compose config
```

### Cannot Remove Container - Volume in Use

**Symptoms:**

```
Error response from daemon: remove orderhub-backend: volume is in use
```

**Solution:**

```bash
# Stop all containers first
docker compose down

# Force remove
docker compose down -v

# Or remove specific volume
docker volume rm orderhub_postgres_data
```

---

## Development Issues

### Maven Build Fails

**Symptoms:**

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solutions:**

```bash
# 1. Clean and rebuild
mvn clean install

# 2. Update dependencies
mvn clean install -U

# 3. Skip tests if they're failing
mvn clean install -DskipTests

# 4. Check Java version
java -version  # Should be 17+

# 5. Clear Maven cache
rm -rf ~/.m2/repository
mvn clean install
```

### Tests Failing

**Symptoms:**

```
[ERROR] Tests run: 10, Failures: 2, Errors: 1, Skipped: 0
```

**Solutions:**

```bash
# 1. Run specific test
mvn test -Dtest=OrderServiceTest

# 2. Run with more output
mvn test -X

# 3. Check if Testcontainers Docker is running
docker ps

# 4. Clean test data
mvn clean test

# 5. Check test database setup
# Tests should use H2 or Testcontainers, not dev database
```

### Hot Reload Not Working

**Symptoms:**

- Code changes not reflected
- Need to restart manually

**Solution:**

```bash
# 1. Add Spring DevTools to pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>

# 2. Enable automatic build in IDE
# IntelliJ: Build > Build Project Automatically

# 3. Run in dev mode
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### IDE Not Recognizing Lombok

**Symptoms:**

- Compilation errors on getters/setters
- Red underlines in IDE

**Solution:**

```bash
# IntelliJ IDEA:
# 1. Install Lombok plugin
# File > Settings > Plugins > Search "Lombok" > Install
# 2. Enable annotation processing
# File > Settings > Build > Compiler > Annotation Processors
# Check "Enable annotation processing"

# Eclipse:
# 1. Download lombok.jar
# 2. Run: java -jar lombok.jar
# 3. Point to Eclipse installation
```

---

## Getting Help

If you can't resolve an issue:

1. **Check logs:** `docker compose logs backend`
2. **Review documentation:** [DOCUMENTATION.md](DOCUMENTATION.md)
3. **Check Swagger UI:** http://localhost:8080/swagger-ui.html
4. **Search error message** in project issues
5. **Create bug report** with:
   - Exact error message
   - Steps to reproduce
   - Application logs
   - Environment details (OS, Java version, etc.)

---

**Still stuck?** Check the [Developer Guide](DEVELOPER_GUIDE.md) or [Deployment Guide](DEPLOYMENT_GUIDE.md) for more details 😊
