# OrderHub MVP — Feature Plan Overview

This directory contains the implementation plan split into individual features. Each file is a self-contained feature with its own tasks, verification steps, and dependency declarations.

## Feature Index

| #   | Feature                                                                 | Priority   | Dependencies       |
| --- | ----------------------------------------------------------------------- | ---------- | ------------------ |
| 01  | [Backend Scaffolding](01-backend-scaffolding.md)                        | Foundation | None               |
| 02  | [Docker Infrastructure](02-docker-infrastructure.md)                    | Foundation | None               |
| 03  | [Angular Scaffolding](03-angular-scaffolding.md)                        | Foundation | None               |
| 04  | [Database Schema](04-database-schema.md)                                | Foundation | 01, 02             |
| 05  | [Common Module](05-common-module.md)                                    | Foundation | 01, 04             |
| 06  | [Auth Backend](06-auth-backend.md)                                      | Core       | 05                 |
| 07  | [Catalog Backend](07-catalog-backend.md)                                | Core       | 05, 06             |
| 08  | [Inventory Backend](08-inventory-backend.md)                            | Core       | 05, 07             |
| 09  | [Orders Backend](09-orders-backend.md)                                  | Core       | 06, 07, 08         |
| 10  | [Payments Backend](10-payments-backend.md)                              | Core       | 09                 |
| 11  | [Backend Testing](11-backend-testing.md)                                | Quality    | 06, 07, 08, 09, 10 |
| 12  | [Frontend Core](12-frontend-core.md)                                    | Frontend   | 03, 06             |
| 13  | [Frontend Auth & Catalog](13-frontend-auth-catalog.md)                  | Frontend   | 12, 07             |
| 14  | [Frontend Cart, Orders & Payments](14-frontend-cart-orders-payments.md) | Frontend   | 13, 09, 10         |
| 15  | [Frontend Admin](15-frontend-admin.md)                                  | Frontend   | 13, 08             |
| 16  | [Integration & Polish](16-integration-polish.md)                        | Final      | All                |

## Dependency Graph

```
01-backend-scaffolding ──┐
                         ├── 04-database-schema ── 05-common-module ── 06-auth-backend ──┬── 07-catalog-backend ── 08-inventory-backend ──┐
02-docker-infrastructure ┘                                                                │                                               │
                                                                                          │                                               │
                                                                                          │            09-orders-backend ─────────────── 10-payments-backend
                                                                                          │            (depends on 06, 07, 08)            │
                                                                                          │                                               │
03-angular-scaffolding ───── 12-frontend-core ── 13-frontend-auth-catalog ──┬── 14-frontend-cart-orders-payments                          │
                             (depends on 03, 06)  (depends on 12, 07)       │   (depends on 13, 09, 10)                                   │
                                                                            │                                                             │
                                                                            ├── 15-frontend-admin                                         │
                                                                            │   (depends on 13, 08)                                       │
                                                                            │                                                             │
                                                                            └──────────────────────── 11-backend-testing ─────────────────┘
                                                                                                      (depends on 06-10)

                                                        16-integration-polish (depends on ALL)
```

## Parallelization Strategy

### Wave 1 — Foundation (all parallel)

```
┌─────────────────────────┐  ┌──────────────────────────┐  ┌─────────────────────────┐
│ 01 Backend Scaffolding  │  │ 02 Docker Infrastructure │  │ 03 Angular Scaffolding  │
└─────────────────────────┘  └──────────────────────────┘  └─────────────────────────┘
```

These three have **zero dependencies** on each other. Start all three simultaneously.

### Wave 2 — Database

```
┌──────────────────────┐
│ 04 Database Schema   │  ← Needs 01 + 02 complete
└──────────────────────┘
```

Requires backend project and Docker (PostgreSQL) to be running.

### Wave 3 — Common + Auth

```
┌──────────────────────┐       ┌──────────────────────┐
│ 05 Common Module     │  ──►  │ 06 Auth Backend      │
└──────────────────────┘       └──────────────────────┘
```

Sequential within this wave: Common must finish before Auth starts.

### Wave 4 — Backend domain modules (partially parallel)

```
┌──────────────────────┐
│ 07 Catalog Backend   │  ← Needs 05, 06
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ 08 Inventory Backend │  ← Needs 07 (catalog creates inventory records)
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐       ┌──────────────────────┐
│ 09 Orders Backend    │  ──►  │ 10 Payments Backend  │
└──────────────────────┘       └──────────────────────┘
```

These are sequential: Catalog → Inventory → Orders → Payments.

### Wave 5 — Frontend + Backend Testing (parallel tracks)

```
Track A (Frontend):                          Track B (Backend Testing):
┌──────────────────────┐                     ┌──────────────────────┐
│ 12 Frontend Core     │                     │ 11 Backend Testing   │
└──────────┬───────────┘                     └──────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│ 13 Frontend Auth & Catalog   │
└──────┬───────────────┬───────┘
       │               │
       ▼               ▼
┌─────────────────┐  ┌────────────────┐
│ 14 FE Cart/     │  │ 15 FE Admin    │   ← These two are PARALLEL
│ Orders/Payments │  │                │
└─────────────────┘  └────────────────┘
```

**Track A** and **Track B** can run in parallel since they are independent.
Within Track A, features 14 and 15 can run in parallel after 13 completes.

### Wave 6 — Final

```
┌──────────────────────────┐
│ 16 Integration & Polish  │  ← Needs everything complete
└──────────────────────────┘
```

## Critical Path

The longest sequential chain determines the minimum implementation timeline:

```
01 → 04 → 05 → 06 → 07 → 08 → 09 → 10 → 14 → 16
```

Shortening this chain is the key to faster delivery. The backend domain features (07-10) are the bottleneck — they must be sequential due to inter-module dependencies.

## How to Use These Plans

1. **Pick a feature** from the table above
2. **Check its dependencies** — all listed features must be complete first
3. **Follow the tasks** in order within the feature file
4. **Run the verification steps** at the bottom before marking complete
5. **Update the checkbox** in this overview when done

## Completion Tracker

- [x] 01 — Backend Scaffolding
- [ ] 02 — Docker Infrastructure
- [ ] 03 — Angular Scaffolding
- [ ] 04 — Database Schema
- [ ] 05 — Common Module
- [ ] 06 — Auth Backend
- [ ] 07 — Catalog Backend
- [ ] 08 — Inventory Backend
- [ ] 09 — Orders Backend
- [ ] 10 — Payments Backend
- [ ] 11 — Backend Testing
- [ ] 12 — Frontend Core
- [ ] 13 — Frontend Auth & Catalog
- [ ] 14 — Frontend Cart, Orders & Payments
- [ ] 15 — Frontend Admin
- [ ] 16 — Integration & Polish
