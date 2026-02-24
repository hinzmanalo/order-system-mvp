# OrderHub MVP - Free Hosting Research & Recommendations

**Research Date**: February 14, 2026  
**Purpose**: Identify the best free hosting solutions for OrderHub MVP portfolio deployment

---

## Executive Summary

After comprehensive research, the **recommended free hosting stack** for OrderHub MVP is:

- **Database**: Neon PostgreSQL (Free Tier)
- **Backend**: Render Web Service (Free Tier)
- **Frontend**: Netlify (Free Tier)

**Total Monthly Cost**: $0 (100% free for portfolio/demo purposes)

---

## 1. Database Hosting: PostgreSQL

### 🏆 RECOMMENDED: Neon (Free Tier)

**Website**: https://neon.com  
**Pricing Page**: https://neon.com/pricing

#### Free Tier Benefits

- ✅ **100 projects** (each with independent database)
- ✅ **0.5 GB storage per project** (sufficient for MVP demo)
- ✅ **100 CU-hours per project/month** (compute units)
- ✅ **Auto-scaling** to zero when inactive (saves resources)
- ✅ **Up to 2 CU (8 GB RAM)** max size
- ✅ **Branching support** (database git-like workflow)
- ✅ **6-hour restore window**
- ✅ **Connection pooling** up to 10,000 connections
- ✅ **PostgreSQL extensions** (pg_vector, PostGIS, TimescaleDB)
- ✅ **No credit card required**
- ✅ **No time limit** (truly free forever)

#### Technical Details

- **Region**: Multiple US/EU regions available
- **Connection**: Standard PostgreSQL connection string
- **Scale to Zero**: Automatic after 5 minutes of inactivity
- **Cold Start**: ~350ms wake-up time
- **Backup**: Automated backups included

#### Perfect For OrderHub Because:

1. Scales to zero = no waste when demo isn't active
2. 0.5GB is enough for product catalog + demo orders
3. 100 CU-hours/month = ~3 hours/day of active usage
4. Supports all PostgreSQL features needed (UUID, JSONB, etc.)
5. Flyway migrations work perfectly

#### Migration Path

```
Local Dev → Neon Free → Neon Launch ($15/mo) → Neon Scale ($701/mo)
```

---

### Alternative: Supabase (Free Tier)

**Website**: https://supabase.com

#### Free Tier

- 500 MB database storage
- 2 GB file storage
- 1 GB bandwidth
- 50,000 monthly active users (auth)
- Up to 500 MB database
- Auto-pause after 1 week of inactivity

**Pros**: Built-in auth, real-time subscriptions, storage  
**Cons**: Smaller storage (500MB), pauses after 1 week inactivity, more complex than needed

---

### Alternative: Render PostgreSQL (Free)

**Website**: https://render.com

#### Free Tier

- 256 MB RAM
- 0.1 CPU
- 100 connections
- **30-day limit** ⚠️

**Pros**: Simple setup, integrates with Render services  
**Cons**: Only lasts 30 days, very small (256MB), expires after a month

---

### ❌ NOT RECOMMENDED

- **ElephantSQL**: Service discontinued (as of 2026)
- **Railway DB**: No truly free tier (requires $5/month minimum)
- **Fly.io Postgres**: Usage-based pricing, no free tier

---

## 2. Backend Hosting: Java Spring Boot

### 🏆 RECOMMENDED: Render Web Service (Free Tier)

**Website**: https://render.com  
**Pricing**: https://render.com/pricing

#### Free Tier Benefits

- ✅ **512 MB RAM**
- ✅ **0.1 CPU**
- ✅ **No time limit** (unlike 30-day DB limit)
- ✅ **Auto-deploy from GitHub**
- ✅ **Zero-downtime deploys**
- ✅ **HTTPS/TLS included**
- ✅ **Custom domains** (upgrade needed)
- ✅ **Docker support** (perfect for your Dockerfile)
- ✅ **Environment variables** management
- ✅ **Health checks** included
- ✅ **7-day log retention**

#### Technical Details

- **Build**: Automatically detects Dockerfile or Maven
- **Deploy**: Push to GitHub = auto deploy
- **Spins down**: After 15 minutes of inactivity
- **Cold start**: ~30-60 seconds wake-up
- **Public URL**: `your-app.onrender.com`

#### Configuration for OrderHub

```yaml
# render.yaml
services:
  - type: web
    name: orderhub-backend
    env: docker
    dockerfilePath: ./backend/Dockerfile
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: DATABASE_URL
        fromDatabase:
          name: orderhub-db
          property: connectionString
    healthCheckPath: /api/v1/health
```

#### Perfect For OrderHub Because:

1. 512MB RAM is sufficient for Spring Boot app
2. Auto-scaling prevents overuse
3. GitHub integration = CI/CD built-in
4. Docker support = use existing Dockerfile
5. Connects easily to Neon PostgreSQL
6. Free HTTPS for API endpoints

#### Limitations to Note

⚠️ **Spins down after 15 min** → Cold starts on first request  
⚠️ **Limited CPU** → May be slow under heavy load  
⚠️ **Public repos only** on free tier

---

### Alternative: Railway (Hobby Tier)

**Website**: https://railway.com  
**Pricing**: https://railway.com/pricing

#### Hobby Tier ($5/month)

- $5 minimum + $5 credits included = **effectively costs usage only**
- Up to 48 vCPU / 48 GB RAM
- Up to 5 GB storage
- **30-day free trial** with $5 credits (no card required)
- Usage-based: $0.00000772 per vCPU/second

**Pros**: More powerful, faster cold starts, better for production  
**Cons**: Not truly free (requires $5/month after trial)

---

### Alternative: Fly.io

**Website**: https://fly.io

#### Pricing

- Usage-based, no free tier
- Very affordable ($3-10/month for small apps)
- Excellent performance

**Pros**: Global edge deployment, fast, great DevX  
**Cons**: No free tier, requires credit card

---

### ❌ NOT RECOMMENDED for Free Tier

- **Vercel**: Optimized for serverless/Node.js, not for Java Spring Boot
- **Netlify Functions**: For serverless only, not full Spring Boot apps
- **Heroku**: No more free tier (discontinued 2022)

---

## 3. Frontend Hosting: Angular

### 🏆 RECOMMENDED: Netlify (Free Tier)

**Website**: https://netlify.com  
**Pricing**: https://www.netlify.com/pricing

#### Free Tier Benefits

- ✅ **100 GB bandwidth/month**
- ✅ **300 build minutes/month**
- ✅ **Unlimited sites**
- ✅ **Auto-deploy from Git**
- ✅ **Instant rollbacks**
- ✅ **Global CDN**
- ✅ **HTTPS/SSL automatic**
- ✅ **Custom domains** (1 free)
- ✅ **Environment variables**
- ✅ **Deploy previews** for PRs
- ✅ **Edge functions** available
- ✅ **Forms** handling (500 submissions/month)

#### Technical Details

- **Build Command**: `npm run build`
- **Publish Directory**: `dist/frontend/browser`
- **Deploy**: Push to GitHub = auto deploy
- **Deploy Time**: ~2-3 minutes
- **URL**: `your-app.netlify.app`

#### Configuration for OrderHub

```toml
# netlify.toml
[build]
  base = "frontend/"
  command = "npm run build"
  publish = "dist/frontend/browser"

[[redirects]]
  from = "/api/*"
  to = "https://orderhub-backend.onrender.com/api/:splat"
  status = 200
  force = true

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

#### Perfect For OrderHub Because:

1. Optimized for Angular SPAs
2. Global CDN = fast worldwide
3. PR previews = test before merge
4. Redirects handle Angular routing
5. Can proxy API calls to backend
6. 100 GB bandwidth = plenty for portfolio

---

### Alternative: Vercel (Free Tier)

**Website**: https://vercel.com

#### Free Tier

- 100 GB bandwidth
- Serverless functions (100 GB-hours)
- Automatic HTTPS
- Global CDN
- Deploy previews

**Pros**: Excellent performance, great DX, NextJS-optimized  
**Cons**: Better for NextJS, similar to Netlify

---

### Alternative: Render Static Sites

**Website**: https://render.com

#### Free Tier

- Unlimited static sites
- Auto-deploy from Git
- Custom domains with TLS
- Lightning-fast CDN

**Pros**: Same platform as backend, simple setup  
**Cons**: Less features than Netlify/Vercel

---

## Recommended Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    USERS / PORTFOLIO                    │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
           ┌────────────────────────┐
           │   Netlify (Frontend)   │
           │  orderhub.netlify.app  │
           │                        │
           │  • Angular 17 SPA      │
           │  • Global CDN          │
           │  • HTTPS included      │
           │  • PR previews         │
           └────────────┬───────────┘
                        │ HTTPS/REST API
                        ▼
           ┌────────────────────────┐
           │   Render (Backend)     │
           │ orderhub-api.render.com│
           │                        │
           │  • Spring Boot 3.2     │
           │  • 512MB RAM           │
           │  • Docker container    │
           │  • Auto-scaling        │
           └────────────┬───────────┘
                        │ PostgreSQL
                        ▼
           ┌────────────────────────┐
           │   Neon (Database)      │
           │   neon.tech/orderhub   │
           │                        │
           │  • PostgreSQL 16       │
           │  • 0.5 GB storage      │
           │  • Auto-pause          │
           │  • Point-in-time       │
           └────────────────────────┘
```

---

## Implementation Plan

### Phase 1: Database Setup (Neon)

1. **Sign up at Neon**: https://console.neon.tech/signup
2. **Create project**: "orderhub-mvp"
3. **Get connection string**:
   ```
   postgresql://user:password@ep-xxx.neon.tech/orderhub?sslmode=require
   ```
4. **Update application-prod.yml**:
   ```yaml
   spring:
     datasource:
       url: ${DATABASE_URL}
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
   ```
5. **Run Flyway migrations**: Will execute on first deploy

### Phase 2: Backend Setup (Render)

1. **Sign up at Render**: https://dashboard.render.com/register
2. **New Web Service** → Connect GitHub repo
3. **Configure**:
   - Name: `orderhub-backend`
   - Environment: `Docker`
   - Dockerfile path: `./backend/Dockerfile`
   - Instance: `Free`
4. **Environment Variables**:
   ```
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=<neon-connection-string>
   JWT_SECRET=<generate-secure-secret>
   ```
5. **Deploy**: Auto-deploys on git push

### Phase 3: Frontend Setup (Netlify)

1. **Sign up at Netlify**: https://app.netlify.com/signup
2. **New site from Git** → Connect GitHub repo
3. **Configure**:
   - Base directory: `frontend`
   - Build command: `npm run build`
   - Publish directory: `dist/frontend/browser`
4. **Environment Variables**:
   ```
   API_URL=https://orderhub-backend.onrender.com
   ```
5. **Deploy**: Auto-deploys on git push

### Phase 4: Testing & Validation

1. ✅ Frontend loads at `https://orderhub.netlify.app`
2. ✅ Can register new user
3. ✅ Can browse products
4. ✅ Can add to cart & checkout
5. ✅ Admin panel works
6. ✅ Database persists data
7. ✅ Cold start recovery works

---

## Cost Breakdown

| Service          | Plan | Monthly Cost | Annual Cost |
| ---------------- | ---- | ------------ | ----------- |
| Neon DB          | Free | $0           | $0          |
| Render Backend   | Free | $0           | $0          |
| Netlify Frontend | Free | $0           | $0          |
| **TOTAL**        |      | **$0**       | **$0**      |

### When to Upgrade

**Neon → Launch ($15/mo)** when:

- Need > 0.5GB storage
- Need > 100 CU-hours/month
- Want 7-day restore window

**Render → Starter ($7/mo)** when:

- Need faster response times
- Want to eliminate cold starts
- Need > 512MB RAM

**Netlify → Pro ($19/mo)** when:

- Need > 100GB bandwidth
- Want team collaboration
- Need custom headers/redirects

---

## Performance Considerations

### Cold Starts

- **Neon DB**: ~350ms wake from sleep
- **Render Backend**: ~30-60s wake from sleep
- **Netlify Frontend**: No cold starts (always on CDN)

### Mitigation Strategies

1. **Keep-alive pings**: Ping backend every 10 minutes (cron job)
2. **Uptime monitoring**: Use UptimeRobot (free) to ping every 5 min
3. **User expectation**: Add "Waking up..." message on first load

### Expected Performance

- **First visit**: 30-60s (backend cold start)
- **Subsequent**: < 500ms (everything warm)
- **Frontend**: ~100ms (CDN cached)

---

## Security Considerations

### SSL/TLS

- ✅ All platforms provide free HTTPS
- ✅ Netlify: Auto-managed Let's Encrypt
- ✅ Render: Auto-managed certificates
- ✅ Neon: SSL-required connections

### Secrets Management

- Use environment variables (never commit)
- Rotate JWT secrets regularly
- Use strong database passwords (generated)

### CORS Configuration

```java
@Configuration
public class SecurityConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://orderhub.netlify.app",
            "http://localhost:4200"  // local dev
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // ...
    }
}
```

---

## Monitoring & Observability

### Free Monitoring Tools

1. **UptimeRobot** (https://uptimerobot.com)
   - Free: 50 monitors
   - Check every 5 minutes
   - Email/SMS alerts

2. **Render Dashboard**
   - Built-in metrics
   - 7-day logs
   - Deploy history

3. **Netlify Analytics** (Paid - $9/mo)
   - Optional upgrade for detailed stats

4. **Neon Console**
   - Database metrics
   - Query performance
   - Storage usage

---

## CI/CD Pipeline

### Automated Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy OrderHub MVP

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      # Backend auto-deploys via Render webhook
      # Frontend auto-deploys via Netlify webhook

      - name: Notify deployment
        run: echo "Deployment triggered"
```

**How it works**:

1. Push to `main` branch
2. Render detects change → builds backend Docker image → deploys
3. Netlify detects change → builds Angular app → deploys to CDN
4. Both complete in ~5 minutes

---

## Demo Data Seeding

### Production Seed Script

Create `backend/src/main/resources/db/seed/prod-seed.sql`:

```sql
-- Seed demo products
INSERT INTO products (id, name, description, price, sku, status) VALUES
('11111111-1111-1111-1111-111111111111', 'Demo Product 1', 'Portfolio demo item', 29.99, 'DEMO-001', 'ACTIVE'),
('22222222-2222-2222-2222-222222222222', 'Demo Product 2', 'Portfolio demo item', 49.99, 'DEMO-002', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- Seed inventory
INSERT INTO inventory (id, product_id, quantity, reserved_quantity) VALUES
('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 100, 0),
('44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 50, 0)
ON CONFLICT DO NOTHING;

-- Create demo admin user
INSERT INTO users (id, email, password_hash, first_name, last_name, role) VALUES
('55555555-5555-5555-5555-555555555555', 'admin@orderhub.demo',
 '$2a$10$...' /* BCrypt hash of 'demo123' */,
 'Admin', 'Demo', 'ADMIN')
ON CONFLICT DO NOTHING;
```

Run on first deploy or via Neon SQL Editor.

---

## Alternative: Upgrade Path

If free tier limitations are hit:

### Option A: Incremental Upgrades

- Start: $0/month
- Phase 1: Render Starter ($7/mo) = faster backend
- Phase 2: Neon Launch ($15/mo) = more storage
- **Total**: ~$22/month

### Option B: Railway Hobby

- **$5/month** for all services
- Single platform
- Better performance
- Simpler management

### Option C: Self-hosting

- VPS (DigitalOcean, Linode): $6/month
- Full control
- More maintenance

---

## Pros & Cons Summary

### ✅ Pros of This Stack

1. **100% Free** for portfolio/demo
2. **Modern architecture** (cloud-native)
3. **Auto-scaling** (Neon, Render)
4. **CI/CD built-in** (GitHub integration)
5. **HTTPS everywhere** (secure by default)
6. **Global CDN** (Netlify)
7. **Easy upgrades** (clear path to paid)
8. **No credit card** required initially
9. **Portfolio-ready** URLs

### ⚠️ Cons to Consider

1. **Cold starts** (30-60s on first request)
2. **Limited resources** (512MB RAM backend)
3. **Auto-pause** (after inactivity)
4. **No custom domains** on free (Netlify allows 1)
5. **Public repos** only (Render free tier)
6. **Limited bandwidth** (100GB/month)

---

## Conclusion

The **Neon + Render + Netlify** stack is the **best free solution** for hosting OrderHub MVP as a portfolio project in 2026:

- ✅ Zero cost
- ✅ Production-ready architecture
- ✅ Auto-scaling and management
- ✅ Professional URLs
- ✅ Easy to upgrade when needed
- ✅ Supports all OrderHub features
- ✅ GitHub-integrated CI/CD

This setup will impress recruiters and showcase your full-stack skills while keeping costs at **$0/month**.

---

## Next Steps

1. ✅ Review this research
2. ⏭️ Create deployment guide using these platforms
3. ⏭️ Set up accounts (Neon, Render, Netlify)
4. ⏭️ Configure environment variables
5. ⏭️ Deploy and test
6. ⏭️ Add monitoring (UptimeRobot)
7. ⏭️ Document URLs in README

Ready to proceed with deployment guide creation?
