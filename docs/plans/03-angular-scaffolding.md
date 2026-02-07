# Feature 03: Angular Scaffolding

**Priority**: Foundation
**Dependencies**: None
**Parallel with**: 01-backend-scaffolding, 02-docker-infrastructure
**Blocks**: 12-frontend-core

---

## Overview

Initialize the Angular 17+ project with standalone components, SCSS styling, routing, and a proxy configuration to forward API requests to the backend.

## User Stories

- N/A (infrastructure)

## Tasks

### 3.1 Project initialization

- [ ] Run `ng new frontend --standalone --style=scss --routing --ssr=false` (or equivalent)
- [ ] Verify Angular 17+ version in `package.json`
- [ ] Verify standalone component setup (no `app.module.ts`)

### 3.2 Proxy configuration

- [ ] Create `frontend/proxy.conf.json`:
  ```json
  {
    "/api": {
      "target": "http://localhost:8080",
      "secure": false,
      "changeOrigin": true
    }
  }
  ```
- [ ] Update `angular.json` → `serve.options.proxyConfig` to reference `proxy.conf.json`

### 3.3 Environment files

- [ ] Create `frontend/src/environments/environment.ts`:
  - `apiUrl: '/api/v1'`
  - `production: true`
- [ ] Create `frontend/src/environments/environment.development.ts`:
  - `apiUrl: '/api/v1'`
  - `production: false`

### 3.4 Git ignores

- [ ] Update root `.gitignore` to include:
  - `frontend/node_modules/`
  - `frontend/dist/`
  - `frontend/.angular/`

### 3.5 Clean up defaults

- [ ] Clear default content from `app.component.ts` template (replace with `<router-outlet>`)
- [ ] Clear default `styles.scss` (leave empty or minimal reset)

## Verification

- [ ] `cd frontend && npm install` completes successfully
- [ ] `ng serve` starts dev server on `http://localhost:4200`
- [ ] `ng build` produces output in `frontend/dist/`
- [ ] Proxy config is picked up (visible in `ng serve` console output)

## Files Created/Modified

```
frontend/
├── angular.json          (modified: proxy config)
├── package.json
├── tsconfig.json
├── proxy.conf.json       (new)
└── src/
    ├── app/
    │   ├── app.component.ts
    │   ├── app.config.ts
    │   └── app.routes.ts
    ├── environments/
    │   ├── environment.ts
    │   └── environment.development.ts
    └── styles.scss
```
