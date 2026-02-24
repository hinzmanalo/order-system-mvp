# OrderHub MVP - Deployment Guide

**Last Updated**: February 14, 2026  
**Deployment Stack**: Neon + Render + Netlify (100% Free)  
**Estimated Time**: 30-45 minutes

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture Overview](#architecture-overview)
3. [Step 1: Database Setup (Neon)](#step-1-database-setup-neon)
4. [Step 2: Backend Deployment (Render)](#step-2-backend-deployment-render)
5. [Step 3: Frontend Deployment (Netlify)](#step-3-frontend-deployment-netlify)
6. [Step 4: Environment Configuration](#step-4-environment-configuration)
7. [Step 5: Testing & Validation](#step-5-testing--validation)
8. [Step 6: Monitoring Setup](#step-6-monitoring-setup)
9. [Maintenance & Updates](#maintenance--updates)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Accounts (All Free)

- ✅ GitHub account (code repository)
- ✅ Neon account: https://console.neon.tech/signup
- ✅ Render account: https://dashboard.render.com/register
- ✅ Netlify account: https://app.netlify.com/signup

### Local Setup

- ✅ Git installed and configured
- ✅ Code pushed to GitHub repository
- ✅ Java 17+ (for local testing)
- ✅ Node.js 18+ (for local testing)
- ✅ Maven (for local testing)

### Pre-Deployment Checklist

- [ ] All unit tests passing (`mvn test`)
- [ ] Application runs locally with Docker Compose
- [ ] Frontend builds successfully (`npm run build`)
- [ ] Environment variables documented
- [ ] GitHub repository is public (or Render/Netlify paid plan)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                  End Users / Portfolio                   │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
           ┌────────────────────────┐
           │   Netlify CDN          │
           │  orderhub.netlify.app  │ ← Frontend (Angular 17)
           │                        │
           │  • Global edge servers │
           │  • HTTPS auto          │
           │  • ~100ms response     │
           └────────────┬───────────┘
                        │ REST API calls
                        ▼
           ┌────────────────────────┐
           │   Render Web Service   │
           │orderhub-api.onrender.com│ ← Backend (Spring Boot)
           │                        │
           │  • 512MB RAM           │
           │  • Docker container    │
           │  • Auto-scale to zero  │
           └────────────┬───────────┘
                        │ PostgreSQL connection
                        ▼
           ┌────────────────────────┐
           │   Neon Database        │
           │  ep-xxx.neon.tech      │ ← Database (PostgreSQL 16)
           │                        │
           │  • 0.5 GB storage      │
           │  • Auto-pause on idle  │
           │  • SSL required        │
           └────────────────────────┘
```

**Data Flow**:

1. User visits `https://orderhub.netlify.app`
2. Netlify serves Angular SPA from global CDN
3. Angular makes API calls to `https://orderhub-api.onrender.com/api/v1/*`
4. Render backend connects to Neon PostgreSQL database
5. Response flows back through the chain

---

## Step 1: Database Setup (Neon)

### 1.1 Create Neon Account

1. Visit https://console.neon.tech/signup
2. Sign up with GitHub (recommended) or email
3. Verify your email if required
4. No credit card needed! ✅

### 1.2 Create New Project

1. Click **"Create a project"** or **"New Project"**
2. Fill in project details:
   ```
   Project name: orderhub-mvp
   Postgres version: 16 (recommended)
   Region: Choose closest to your users
     - US East (N. Virginia) - us-east-1
     - EU (Ireland) - eu-west-1
     - Asia Pacific (Singapore) - ap-southeast-1
   ```
3. Click **"Create Project"**
4. Wait ~30 seconds for provisioning

### 1.3 Get Connection Details

After creation, Neon will show connection details:

```bash
# Connection string format
postgresql://[user]:[password]@[hostname]/[database]?sslmode=require

# Example:
postgresql://orderhub_user:AbCd1234...@ep-cool-forest-12345.us-east-1.aws.neon.tech/orderhub?sslmode=require
```

**Save these values** (you'll need them later):

- **Host**: `ep-cool-forest-12345.us-east-1.aws.neon.tech`
- **Database**: `orderhub` (or `neondb`)
- **Username**: `orderhub_user` (or similar)
- **Password**: `AbCd1234...` (auto-generated)
- **Port**: `5432`
- **Connection String**: Full URL above

### 1.4 Configure Database

1. Click on your project **"orderhub-mvp"**
2. Go to **Settings** → **General**
3. Configure settings:
   ```
   Auto-suspend delay: 5 minutes (default)
   Compute size: 0.25 CU min, 2 CU max (free tier)
   ✅ Enable pooling (recommended)
   ```
4. Note the **Pooled connection string** if using connection pooling:
   ```
   postgresql://[user]:[password]@[hostname]/[database]?sslmode=require&pgbouncer=true
   ```

### 1.5 Test Connection (Optional)

Using `psql` locally:

```bash
# Install psql if needed (macOS)
brew install postgresql@16

# Test connection
psql "postgresql://orderhub_user:AbCd1234...@ep-cool-forest-12345.us-east-1.aws.neon.tech/orderhub?sslmode=require"

# You should see:
# psql (16.x)
# SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, bits: 256, compression: off)
# Type "help" for help.
# orderhub=>
```

Type `\q` to exit.

### 1.6 Notes on Neon Free Tier

- ✅ Database auto-pauses after 5 minutes of inactivity
- ✅ Wakes up automatically on first query (~350ms)
- ✅ 100 compute hours per month (sufficient for demo)
- ✅ 0.5 GB storage limit
- ✅ Data persists even when paused
- ⚠️ Connection limit: 100 concurrent connections
- ⚠️ Backup retention: 6 hours

---

## Step 2: Backend Deployment (Render)

### 2.1 Create Render Account

1. Visit https://dashboard.render.com/register
2. Sign up with **GitHub** (recommended for auto-deploy)
3. Authorize Render to access your GitHub repositories
4. No credit card needed! ✅

### 2.2 Prepare Backend for Deployment

Before deploying, ensure your backend is production-ready:

#### A. Create Production Application Properties

**File**: `backend/src/main/resources/application-prod.yml`

```yaml
server:
  port: ${PORT:8080}
  error:
    include-message: always
    include-binding-errors: always

spring:
  application:
    name: orderhub

  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
        show_sql: false
    open-in-view: false

  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    locations: classpath:db/migration

security:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiry: 900000 # 15 minutes
    refresh-token-expiry: 604800000 # 7 days

logging:
  level:
    root: INFO
    com.orderhub: INFO
    org.springframework.web: INFO
    org.hibernate: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

#### B. Update CORS Configuration

**File**: `backend/src/main/java/com/orderhub/auth/security/SecurityConfig.java`

Add production frontend URL:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "https://orderhub.netlify.app",              // Production
        "https://your-custom-domain.com",            // Custom domain (if any)
        "http://localhost:4200"                      // Local development
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

#### C. Verify Dockerfile

**File**: `backend/Dockerfile`

```dockerfile
# Multi-stage build for smaller image
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### D. Commit and Push Changes

```bash
cd /Users/hinomanalo/claude/order-system-mvp

# Add changes
git add backend/src/main/resources/application-prod.yml
git add backend/src/main/java/com/orderhub/auth/security/SecurityConfig.java
git add backend/Dockerfile

# Commit
git commit -m "chore: prepare backend for production deployment"

# Push to GitHub
git push origin main
```

### 2.3 Create Render Web Service

1. **Go to Render Dashboard**: https://dashboard.render.com
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub repository:
   - Click **"Connect account"** if not already connected
   - Find and select **"order-system-mvp"** repository
   - Click **"Connect"**

### 2.4 Configure Web Service

Fill in the deployment configuration:

```
Name: orderhub-backend
Environment: Docker
Region: Oregon (US West) or closest to your Neon DB
Branch: main
```

**Build Settings**:

```
Root Directory: backend
Dockerfile Path: ./Dockerfile
Docker Command: (leave blank - uses ENTRYPOINT from Dockerfile)
```

**Instance Type**:

```
Free (512 MB RAM, 0.1 CPU)
```

### 2.5 Set Environment Variables

Click **"Advanced"** → **"Environment Variables"**

Add the following:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database Connection (from Neon Step 1.3)
DATABASE_URL=postgresql://orderhub_user:AbCd1234...@ep-cool-forest-12345.us-east-1.aws.neon.tech/orderhub?sslmode=require

# Extract username and password from connection string
DB_USERNAME=orderhub_user
DB_PASSWORD=AbCd1234...

# JWT Secret (generate a strong random secret)
JWT_SECRET=your-super-secret-jwt-key-change-this-to-something-random-and-secure-at-least-64-chars

# Server Port (Render provides this automatically)
PORT=8080
```

**Generate Secure JWT Secret**:

```bash
# macOS/Linux - generates 64-char random string
openssl rand -base64 48
# Example output: 3F2504E0-4F89-11D3-9A0C-0305E82C3301-A1B2C3D4E5F6...
```

### 2.6 Configure Health Check

Scroll to **"Health & Alerts"**:

```
Health Check Path: /api/v1/health
```

This ensures Render knows when your app is ready.

### 2.7 Create Auto-Deploy Setting

Under **"Auto-Deploy"**:

```
✅ Yes (Deploy on every push to main branch)
```

### 2.8 Deploy Backend

1. Click **"Create Web Service"**
2. Render will start building your Docker image
3. Watch the build logs in real-time

**Expected build time**: 5-10 minutes

**Build stages**:

```
==> Building Docker image
==> Pulling base images
==> Installing dependencies
==> Compiling application
==> Creating final image
==> Deploying container
==> Health check passed ✓
==> Deploy live
```

### 2.9 Get Backend URL

Once deployed, you'll see:

```
Your service is live at https://orderhub-backend.onrender.com
```

**Save this URL** - you'll need it for frontend configuration.

### 2.10 Test Backend Deployment

```bash
# Test health endpoint
curl https://orderhub-backend.onrender.com/api/v1/health

# Expected response:
# {"status": "UP"}

# Test Swagger UI
open https://orderhub-backend.onrender.com/swagger-ui.html
```

---

## Step 3: Frontend Deployment (Netlify)

### 3.1 Create Netlify Account

1. Visit https://app.netlify.com/signup
2. Sign up with **GitHub** (recommended)
3. Authorize Netlify to access your repositories
4. No credit card needed! ✅

### 3.2 Prepare Frontend for Deployment

#### A. Update Environment Configuration

**File**: `frontend/src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: "https://orderhub-backend.onrender.com/api/v1",
};
```

#### B. Update Angular Build Configuration

**File**: `frontend/angular.json`

Ensure production build is optimized:

```json
{
  "projects": {
    "frontend": {
      "architect": {
        "build": {
          "configurations": {
            "production": {
              "budgets": [
                {
                  "type": "initial",
                  "maximumWarning": "2mb",
                  "maximumError": "5mb"
                }
              ],
              "outputHashing": "all",
              "optimization": true,
              "sourceMap": false,
              "namedChunks": false,
              "aot": true,
              "extractLicenses": true,
              "vendorChunk": false,
              "buildOptimizer": true,
              "fileReplacements": [
                {
                  "replace": "src/environments/environment.ts",
                  "with": "src/environments/environment.prod.ts"
                }
              ]
            }
          }
        }
      }
    }
  }
}
```

#### C. Create Netlify Configuration

**File**: `frontend/netlify.toml` (create this file)

```toml
[build]
  base = "frontend/"
  command = "npm run build"
  publish = "dist/frontend/browser"

# Redirect API calls to backend
[[redirects]]
  from = "/api/*"
  to = "https://orderhub-backend.onrender.com/api/:splat"
  status = 200
  force = true
  headers = {X-From = "Netlify"}

# SPA routing - redirect all routes to index.html
[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200

# Headers for security
[[headers]]
  for = "/*"
  [headers.values]
    X-Frame-Options = "DENY"
    X-Content-Type-Options = "nosniff"
    X-XSS-Protection = "1; mode=block"
    Referrer-Policy = "strict-origin-when-cross-origin"

# Cache static assets
[[headers]]
  for = "/assets/*"
  [headers.values]
    Cache-Control = "public, max-age=31536000, immutable"
```

#### D. Verify Package.json Scripts

**File**: `frontend/package.json`

```json
{
  "scripts": {
    "build": "ng build --configuration production",
    "build:prod": "ng build --configuration production"
  }
}
```

#### E. Commit and Push Changes

```bash
cd /Users/hinomanalo/claude/order-system-mvp

# Add changes
git add frontend/src/environments/environment.prod.ts
git add frontend/netlify.toml
git add frontend/angular.json

# Commit
git commit -m "chore: configure frontend for Netlify deployment"

# Push
git push origin main
```

### 3.3 Create Netlify Site

1. **Go to Netlify Dashboard**: https://app.netlify.com
2. Click **"Add new site"** → **"Import an existing project"**
3. Select **"Deploy with GitHub"**
4. Find and select **"order-system-mvp"** repository
5. Click the repository to connect

### 3.4 Configure Build Settings

Netlify will try to auto-detect settings. Verify/update:

```
Branch to deploy: main
Base directory: frontend
Build command: npm run build
Publish directory: dist/frontend/browser
```

### 3.5 Set Environment Variables

Click **"Show advanced"** → **"New variable"**

Add:

```bash
# API URL (your Render backend)
API_URL=https://orderhub-backend.onrender.com/api/v1

# Angular CLI analytics (optional)
NG_CLI_ANALYTICS=false
```

### 3.6 Deploy Frontend

1. Click **"Deploy site"**
2. Netlify will start building

**Build time**: 3-5 minutes

**Build stages**:

```
==> Preparing build environment
==> Installing Node.js and npm
==> Installing dependencies (npm install)
==> Building Angular app (npm run build)
==> Processing build for deployment
==> Deploy successful ✓
```

### 3.7 Get Frontend URL

Once deployed, you'll see:

```
Your site is live at https://random-name-12345.netlify.app
```

### 3.8 Customize Site Name (Optional)

1. Go to **"Site settings"** → **"General"** → **"Site information"**
2. Click **"Change site name"**
3. Enter: `orderhub` (or any available name)
4. Your new URL: `https://orderhub.netlify.app`

### 3.9 Test Frontend Deployment

```bash
# Open in browser
open https://orderhub.netlify.app

# Should see:
# ✅ OrderHub home page loads
# ✅ Product catalog displays
# ✅ Can navigate to login/register
```

---

## Step 4: Environment Configuration

### 4.1 Update Backend CORS for Production

Go back to your code and update CORS:

**File**: `backend/src/main/java/com/orderhub/auth/security/SecurityConfig.java`

```java
config.setAllowedOrigins(List.of(
    "https://orderhub.netlify.app",    // ← Update with your actual Netlify URL
    "http://localhost:4200"
));
```

Commit and push:

```bash
git add backend/src/main/java/com/orderhub/auth/security/SecurityConfig.java
git commit -m "fix: update CORS for production Netlify URL"
git push origin main
```

Render will auto-deploy the update (~5 minutes).

### 4.2 Update Frontend API URL

If you customized your Render backend URL:

**File**: `frontend/src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: "https://orderhub-backend.onrender.com/api/v1", // ← Update if different
};
```

### 4.3 Configure Database Seeding (Optional)

To populate demo data, use Neon's SQL Editor:

1. Go to Neon Console → Your Project
2. Click **"SQL Editor"**
3. Run seed data:

```sql
-- Create demo admin user (password: demo123)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'admin@orderhub.demo',
  '$2a$10$rU8P5rR5lP.yXfN6v8fB.u4LQ8K3XhXnN9v2qW8yZ9vK5L3L5Q5Ky',  -- BCrypt hash of 'demo123'
  'Admin',
  'Demo',
  'ADMIN',
  NOW(),
  NOW()
) ON CONFLICT (email) DO NOTHING;

-- Create demo products
INSERT INTO products (id, name, description, price, sku, status, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 'MOUSE-001', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'Mechanical Keyboard', 'RGB backlit mechanical keyboard', 89.99, 'KEYB-001', 'ACTIVE', NOW(), NOW()),
  (gen_random_uuid(), 'USB-C Cable', 'High-speed USB-C to USB-C cable', 14.99, 'CABLE-001', 'ACTIVE', NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;

-- Add inventory for products
INSERT INTO inventory (id, product_id, quantity, reserved_quantity, created_at, updated_at, version)
SELECT
  gen_random_uuid(),
  p.id,
  100,
  0,
  NOW(),
  NOW(),
  0
FROM products p
ON CONFLICT DO NOTHING;
```

Click **"Run"** to execute.

---

## Step 5: Testing & Validation

### 5.1 End-to-End Testing

**Test User Registration**:

1. Visit https://orderhub.netlify.app
2. Click **"Register"**
3. Fill form:
   ```
   Email: test@example.com
   Password: Test123!
   First Name: Test
   Last Name: User
   ```
4. Submit
5. ✅ Should redirect to login

**Test User Login**:

1. Login with credentials above
2. ✅ Should redirect to products page
3. ✅ Should see "Welcome, Test User" or similar

**Test Product Catalog**:

1. ✅ Products should load and display
2. ✅ Can view product details
3. ✅ Can filter/search products

**Test Cart & Checkout**:

1. Add products to cart
2. ✅ Cart count updates
3. Navigate to cart
4. ✅ Cart items display
5. Proceed to checkout
6. ✅ Can complete order

**Test Admin Panel** (if seeded):

1. Logout
2. Login as `admin@orderhub.demo` / `demo123`
3. Navigate to Admin panel
4. ✅ Can view products
5. ✅ Can manage inventory
6. ✅ Can view orders

### 5.2 Performance Testing

**Check Response Times**:

```bash
# Test backend health
time curl https://orderhub-backend.onrender.com/api/v1/health

# First request (cold start): ~30-60 seconds
# Subsequent requests: < 500ms
```

**Check Frontend Load Time**:

1. Open Chrome DevTools → Network tab
2. Hard refresh (Cmd+Shift+R)
3. Check metrics:
   - ✅ DOMContentLoaded: < 2s
   - ✅ Load: < 5s
   - ✅ LCP: < 3s

### 5.3 Mobile Testing

1. Open on mobile browser
2. ✅ Responsive layout works
3. ✅ Touch interactions work
4. ✅ Forms are usable

### 5.4 Cross-Browser Testing

Test on:

- ✅ Chrome
- ✅ Firefox
- ✅ Safari
- ✅ Edge

---

## Step 6: Monitoring Setup

### 6.1 UptimeRobot Configuration

**Purpose**: Prevent cold starts by pinging backend every 5 minutes

1. **Sign up**: https://uptimerobot.com (free)
2. **Add New Monitor**:
   ```
   Monitor Type: HTTP(s)
   Friendly Name: OrderHub Backend
   URL: https://orderhub-backend.onrender.com/api/v1/health
   Monitoring Interval: 5 minutes
   ```
3. **Add Alert Contacts**:
   - Email: your-email@example.com
   - Alert When: Down

4. **Repeat for Frontend**:
   ```
   Monitor Type: HTTP(s)
   Friendly Name: OrderHub Frontend
   URL: https://orderhub.netlify.app
   Monitoring Interval: 5 minutes
   ```

### 6.2 Render Dashboard Monitoring

1. Go to Render Dashboard
2. Click on **"orderhub-backend"**
3. View metrics:
   - CPU usage
   - Memory usage
   - HTTP requests
   - Response times

Set up alerts:

- Navigate to **"Notifications"**
- Enable email alerts for:
  - ✅ Deploy failures
  - ✅ Service crashes
  - ✅ High error rates

### 6.3 Netlify Analytics (Optional - Paid)

For detailed analytics:

1. Go to Netlify site settings
2. Navigate to **"Analytics"**
3. Enable (costs $9/month)

Free alternative: **Google Analytics**

Add to `frontend/src/index.html`:

```html
<!-- Google Analytics -->
<script
  async
  src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"
></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag() {
    dataLayer.push(arguments);
  }
  gtag("js", new Date());
  gtag("config", "G-XXXXXXXXXX");
</script>
```

### 6.4 Neon Database Monitoring

1. Go to Neon Console
2. Click on your project
3. View **"Monitoring"** tab:
   - Connection count
   - Query performance
   - Storage usage
   - Compute usage

Set consumption alerts:

- Navigate to **"Settings"** → **"Usage"**
- Set alert at 80% of free tier limits

---

## Maintenance & Updates

### Auto-Deployment Workflow

**Current setup**: Push to `main` = auto-deploy everywhere

```bash
# Make changes locally
git add .
git commit -m "feat: add new feature"
git push origin main

# Automatic deployments:
# 1. Render detects push → rebuilds backend (5 min)
# 2. Netlify detects push → rebuilds frontend (3 min)
```

### Manual Deployment Triggers

**Render**:

1. Go to service dashboard
2. Click **"Manual Deploy"** → **"Deploy latest commit"**

**Netlify**:

1. Go to site dashboard
2. Click **"Trigger deploy"** → **"Deploy site"**

### Environment Variable Updates

**Render**:

1. Service dashboard → **"Environment"**
2. Update variables
3. Click **"Save"**
4. Redeploy (automatic)

**Netlify**:

1. Site settings → **"Environment variables"**
2. Update variables
3. Redeploy site

### Database Migrations

Flyway runs automatically on backend startup:

1. Create new migration: `backend/src/main/resources/db/migration/V7__description.sql`
2. Commit and push
3. Render redeploys backend
4. Migration runs automatically
5. Check logs to confirm

### Rollback Procedure

**Backend (Render)**:

1. Go to service dashboard
2. Click **"Events"** tab
3. Find previous successful deploy
4. Click **"Rollback to this deploy"**

**Frontend (Netlify)**:

1. Go to **"Deploys"** tab
2. Find previous deploy
3. Click **"Publish deploy"**

---

## Troubleshooting

### Backend Issues

#### Problem: Backend won't start

**Symptoms**:

- Build succeeds but service fails health check
- Logs show application errors

**Solutions**:

1. **Check environment variables**:

   ```bash
   # Verify all required env vars are set
   - SPRING_PROFILES_ACTIVE=prod
   - DATABASE_URL=postgresql://...
   - JWT_SECRET=...
   ```

2. **Check database connection**:
   - Test Neon connection string separately
   - Verify SSL mode is `require`
   - Check database is not paused

3. **Review logs**:
   - Render dashboard → Service → **"Logs"**
   - Look for startup errors
   - Check Flyway migration status

#### Problem: Cold starts too slow

**Symptoms**:

- First request after inactivity takes 30-60s

**Solutions**:

1. **Set up UptimeRobot** (see Step 6.1)
   - Pings every 5 minutes
   - Keeps service warm

2. **Upgrade to Render Starter** ($7/mo)
   - No auto-scaling
   - Always on

#### Problem: Out of memory errors

**Symptoms**:

- Service crashes with OOM errors
- `java.lang.OutOfMemoryError` in logs

**Solutions**:

1. **Reduce JVM heap size**:

   ```dockerfile
   # In Dockerfile, update ENTRYPOINT
   ENTRYPOINT ["java", "-Xmx400m", "-Xms200m", "-jar", "/app/app.jar"]
   ```

2. **Optimize Spring Boot**:

   ```yaml
   # application-prod.yml
   spring:
     jpa:
       properties:
         hibernate:
           jdbc:
             batch_size: 10
     datasource:
       hikari:
         maximum-pool-size: 3 # Reduce from 5
   ```

3. **Upgrade instance** (Starter: $7/mo, 512MB → 1GB)

### Frontend Issues

#### Problem: API calls failing (CORS errors)

**Symptoms**:

- Browser console: `CORS policy: No 'Access-Control-Allow-Origin' header`
- API requests fail with 403

**Solutions**:

1. **Verify backend CORS config** includes your Netlify URL:

   ```java
   config.setAllowedOrigins(List.of(
       "https://orderhub.netlify.app",  // ← Must match exactly
       "http://localhost:4200"
   ));
   ```

2. **Check netlify.toml redirects**:

   ```toml
   [[redirects]]
     from = "/api/*"
     to = "https://orderhub-backend.onrender.com/api/:splat"
     status = 200  # Must be 200, not 301/302
     force = true
   ```

3. **Verify environment.prod.ts**:
   ```typescript
   apiUrl: "https://orderhub-backend.onrender.com/api/v1";
   // Must match backend URL exactly
   ```

#### Problem: Routing not working (404 on refresh)

**Symptoms**:

- Clicking links works
- Refreshing page = 404

**Solution**:

Verify `netlify.toml` has SPA redirect:

```toml
[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

#### Problem: Build fails

**Symptoms**:

- Netlify build logs show errors
- TypeScript compilation fails

**Solutions**:

1. **Test build locally**:

   ```bash
   cd frontend
   npm install
   npm run build
   # Fix any errors shown
   ```

2. **Check Node version**:

   ```toml
   # Add to netlify.toml
   [build.environment]
     NODE_VERSION = "18"
   ```

3. **Clear cache and retry**:
   - Netlify dashboard → **"Deploys"**
   - **"Trigger deploy"** → **"Clear cache and deploy site"**

### Database Issues

#### Problem: Connection refused or timeout

**Symptoms**:

- Backend logs: `Connection refused`
- `timeout` errors

**Solutions**:

1. **Verify Neon database is active**:
   - Neon console → Project
   - Check status indicator (should be green)

2. **Test connection string**:

   ```bash
   psql "postgresql://user:pass@host/db?sslmode=require"
   ```

3. **Check IP allowlist** (if configured):
   - Neon doesn't restrict by default
   - Render's IPs change, don't use IP restrictions

#### Problem: Out of storage (0.5 GB limit)

**Symptoms**:

- Neon dashboard shows 100% storage
- Insert operations fail

**Solutions**:

1. **Clean up test data**:

   ```sql
   -- Delete old orders
   DELETE FROM orders WHERE created_at < NOW() - INTERVAL '30 days';

   -- Vacuum database
   VACUUM FULL;
   ```

2. **Upgrade to Neon Launch** ($15/mo):
   - Pay-as-you-go storage
   - No hard limits

#### Problem: Database paused, slow wake-up

**Symptoms**:

- First query after inactivity takes ~5-10 seconds
- Logs show connection delays

**Solutions**:

1. **Keep database active**:
   - UptimeRobot pings backend every 5 min
   - Backend queries DB on health check
   - Prevents auto-pause

2. **Optimize connection pooling**:

   ```yaml
   spring:
     datasource:
       hikari:
         connection-timeout: 30000 # 30 seconds
         validation-timeout: 5000
   ```

3. **Disable scale-to-zero** (Neon Launch plan only)

---

## Performance Optimization

### Backend Optimization

1. **Enable HTTP/2**:

   ```yaml
   # application-prod.yml
   server:
     http2:
       enabled: true
   ```

2. **Enable response compression**:

   ```yaml
   server:
     compression:
       enabled: true
       mime-types: application/json,application/xml,text/html,text/xml,text/plain
   ```

3. **Tune Hikari pool**:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 3 # Low for 512MB RAM
         minimum-idle: 1
         connection-timeout: 20000
   ```

### Frontend Optimization

1. **Enable lazy loading**:

   ```typescript
   // app.routes.ts
   {
     path: 'admin',
     loadComponent: () => import('./features/admin/admin.component')
   }
   ```

2. **Optimize images**:
   - Use WebP format
   - Compress with TinyPNG
   - Lazy load images

3. **Enable service worker** (PWA):
   ```bash
   ng add @angular/pwa
   ```

---

## Scaling Beyond Free Tier

### When to Upgrade

**Neon → Launch ($15/mo)**:

- Storage > 0.5 GB
- Compute > 100 hours/month
- Need longer restore window

**Render → Starter ($7/mo)**:

- Eliminate cold starts
- Faster response times
- More RAM needed

**Netlify → Pro ($19/mo)**:

- > 100 GB bandwidth
- Team collaboration
- Advanced analytics

### Migration to Paid Tiers

**Neon**:

1. Go to project → **"Settings"** → **"Billing"**
2. Click **"Upgrade to Launch"**
3. Add payment method
4. No downtime, instant upgrade

**Render**:

1. Service dashboard → **"Settings"**
2. Change instance type to **"Starter"**
3. Add payment method
4. ~30s downtime during migration

**Netlify**:

1. Site settings → **"Billing"**
2. Upgrade to **"Pro"**
3. Add payment method
4. No downtime, instant upgrade

---

## Security Checklist

- [ ] All connections use HTTPS/TLS
- [ ] JWT secret is strong and random (64+ chars)
- [ ] Database password is strong
- [ ] CORS properly configured (specific origins)
- [ ] Environment variables never committed to Git
- [ ] Sensitive data not logged
- [ ] SQL injection prevented (JPA/Hibernate)
- [ ] XSS protection enabled (Angular sanitizes by default)
- [ ] CSRF protection enabled (Spring Security default)
- [ ] Rate limiting configured (consider Render's DDoS protection)
- [ ] Regular dependency updates (`npm audit`, `mvn dependency:tree`)

---

## Useful Commands

### Development

```bash
# Backend
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd frontend
npm install
ng serve

# Database (local)
docker compose up db -d
```

### Deployment

```bash
# Trigger redeploy
git push origin main

# View logs
# Render: Dashboard → Logs
# Netlify: Dashboard → Deploys → Deploy log

# Test production build locally
cd frontend
npm run build
npx http-server dist/frontend/browser -p 8080
```

### Database

```bash
# Connect to Neon DB
psql "postgresql://user:pass@host/db?sslmode=require"

# Run migration manually
cd backend
mvn flyway:migrate

# Reset database (DANGER!)
mvn flyway:clean flyway:migrate
```

---

## URLs Reference

After deployment, you'll have:

```
Frontend:  https://orderhub.netlify.app
Backend:   https://orderhub-backend.onrender.com
API Docs:  https://orderhub-backend.onrender.com/swagger-ui.html
Database:  ep-xxx-xxx.us-east-1.aws.neon.tech:5432

Health:    https://orderhub-backend.onrender.com/api/v1/health
```

Save these in your README!

---

## Next Steps

- [ ] Custom domain setup (optional, $12/year for .com)
- [ ] Enable HTTPS on custom domain
- [ ] Set up CI/CD testing (GitHub Actions)
- [ ] Add monitoring dashboards
- [ ] Create user documentation
- [ ] Portfolio showcase page

---

## Support & Resources

- **Neon Docs**: https://neon.tech/docs
- **Render Docs**: https://render.com/docs
- **Netlify Docs**: https://docs.netlify.com
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Angular**: https://angular.io/docs

---

**Deployment Complete!** 🎉

Your OrderHub MVP is now live and accessible worldwide at no cost. Share your portfolio with confidence!

Questions? Check the troubleshooting section or community forums.
