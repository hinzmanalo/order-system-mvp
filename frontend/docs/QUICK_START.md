# OrderHub Frontend - Quick Start Guide

This guide will get you up and running with the OrderHub frontend in under 5 minutes.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js** 18.x or higher
- **npm** 9.x or higher
- **Angular CLI** 17.x (optional but recommended)
- **Git**

Check your versions:

```bash
node --version  # Should be >= 18.x
npm --version   # Should be >= 9.x
```

## Installation

### 1. Clone and Navigate

```bash
cd /Users/hinomanalo/claude/order-system-mvp/frontend
```

### 2. Install Dependencies

```bash
npm install
```

This will install all required Angular packages and dependencies.

### 3. Start Backend API (Required)

The frontend requires the backend API to be running. In a separate terminal:

```bash
cd ../backend
mvn spring-boot:run
```

Or use Docker Compose from the project root:

```bash
cd ..
docker compose up db backend -d
```

Verify backend is running at: http://localhost:8080/api/v1/health

### 4. Start Development Server

```bash
npm start
# or: ng serve
```

The application will be available at: **http://localhost:4200**

## First Steps

### Access the Application

1. Open your browser to http://localhost:4200
2. You'll be redirected to the product catalog
3. Click **Register** to create a new account
4. Or use default credentials (if seeded):
   - **User**: `user@orderhub.com` / `password123`
   - **Admin**: `admin@orderhub.com` / `admin123`

### Explore Features

- **Browse Products**: View available products on the main page
- **View Details**: Click any product to see details and inventory
- **Add to Cart**: Requires login
- **Checkout**: Complete a purchase (requires login)
- **Order History**: View your past orders (requires login)
- **Admin Panel**: Manage products, inventory, and orders (admin role only)

## Development Workflow

### Live Reload

The dev server supports hot module replacement. Changes to TypeScript, HTML, or SCSS files will automatically reload the browser.

### API Proxy

The frontend proxies API requests to the backend via `proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

All requests to `/api/*` are forwarded to `http://localhost:8080/api/*`.

## Common Commands

| Command                        | Description                               |
| ------------------------------ | ----------------------------------------- |
| `npm start`                    | Start dev server at http://localhost:4200 |
| `npm run build`                | Build for production                      |
| `npm test`                     | Run unit tests via Karma                  |
| `ng generate component <name>` | Generate new component                    |
| `ng generate service <name>`   | Generate new service                      |
| `npm run lint`                 | Run linter (if configured)                |

## Project Structure Overview

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/           # Singleton services, guards, interceptors
│   │   ├── shared/         # Reusable components, pipes, directives
│   │   ├── features/       # Feature modules (auth, catalog, cart, etc.)
│   │   ├── app.config.ts   # Application providers
│   │   └── app.routes.ts   # Route configuration
│   ├── assets/             # Static assets
│   ├── environments/       # Environment configs
│   └── styles.scss         # Global styles
├── docs/                   # Documentation
├── angular.json            # Angular workspace config
├── package.json            # Dependencies
├── proxy.conf.json         # API proxy config
└── tsconfig.json           # TypeScript config
```

## Troubleshooting

### Backend Connection Issues

**Problem**: API calls fail with connection errors

**Solutions**:

1. Verify backend is running: `curl http://localhost:8080/api/v1/health`
2. Check `proxy.conf.json` configuration
3. Ensure no firewall blocking port 8080

### Port Already in Use

**Problem**: `Port 4200 is already in use`

**Solutions**:

```bash
# Option 1: Kill process on port 4200
lsof -ti:4200 | xargs kill -9

# Option 2: Use different port
ng serve --port 4300
```

### Module Not Found Errors

**Problem**: TypeScript compilation errors after pulling changes

**Solutions**:

```bash
rm -rf node_modules package-lock.json
npm install
```

### Authentication Issues

**Problem**: Can't login or getting 401 errors

**Solutions**:

1. Clear browser cookies and localStorage
2. Verify backend auth service is running
3. Check browser console for detailed error messages
4. Try incognito mode to rule out cached credentials

## Next Steps

Once you're up and running:

1. **Read the Architecture Guide**: [ARCHITECTURE.md](./ARCHITECTURE.md)
2. **Review Development Guide**: [DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)
3. **Understand API Integration**: [API_INTEGRATION.md](./API_INTEGRATION.md)
4. **Learn Component Patterns**: [COMPONENT_GUIDE.md](./COMPONENT_GUIDE.md)

## Getting Help

- **Local Docs**: Check the `docs/` folder
- **Backend API**: http://localhost:8080/swagger-ui.html
- **Angular Docs**: https://angular.io/docs
- **Project Issues**: Check the GitHub repository

---

**Happy coding! 😊**
