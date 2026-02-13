# OrderHub Backend Documentation

Complete documentation for the OrderHub backend REST API.

## 📚 Documentation Index

### Getting Started

- **[README](../README.md)** - Main documentation with features, setup, and API reference
- **[API Quick Reference](docs/API_QUICK_REFERENCE.md)** - Fast endpoint reference with cURL examples

### Development

- **[Developer Guide](docs/DEVELOPER_GUIDE.md)** - Step-by-step guides for common development tasks
  - Adding new endpoints
  - Creating database migrations
  - Writing tests
  - Exception handling
  - Logging best practices

### Deployment

- **[Deployment Guide](docs/DEPLOYMENT_GUIDE.md)** - Production deployment and operations
  - Building for production
  - Docker deployment
  - Database setup
  - Monitoring and logging
  - Scaling strategies

### Architecture Documentation

- **[OrderHub MVP Specification](../../docs/OrderHub_MVP.md)** - Complete MVP specification
- **[Product Requirements](../../docs/prd.md)** - Product requirements document
- **[Implementation Plans](../../docs/plans/)** - Detailed implementation plans by phase

## 🎯 Quick Links

### For New Developers

1. Read [README](../README.md) - Quick Start section
2. Set up environment: `docker compose up --build`
3. Explore API: http://localhost:8080/swagger-ui.html
4. Read [Developer Guide](docs/DEVELOPER_GUIDE.md)

### For DevOps/SRE

1. Review [Deployment Guide](docs/DEPLOYMENT_GUIDE.md)
2. Check environment variables configuration
3. Set up monitoring and health checks
4. Configure backup strategy

### For API Consumers

1. Check [API Quick Reference](docs/API_QUICK_REFERENCE.md)
2. Test endpoints via Swagger UI
3. Review authentication flow
4. Understand error response format

## 🔧 Tech Stack

| Layer                | Technology                  |
| -------------------- | --------------------------- |
| **Runtime**          | Java 17                     |
| **Framework**        | Spring Boot 3.2.2           |
| **Database**         | PostgreSQL 16               |
| **ORM**              | Spring Data JPA + Hibernate |
| **Migrations**       | Flyway                      |
| **Security**         | Spring Security + JWT       |
| **API Docs**         | SpringDoc OpenAPI 3         |
| **Build Tool**       | Maven                       |
| **Containerization** | Docker                      |

## 📖 API Overview

### Public Endpoints (No Auth Required)

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/products` - Browse product catalog
- `GET /api/v1/products/{id}` - Get product details

### Authenticated Endpoints (JWT Required)

- `GET /api/v1/auth/me` - Get current user
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders` - List my orders
- `POST /api/v1/orders/{id}/cancel` - Cancel order
- `POST /api/v1/orders/{orderId}/payments` - Process payment
- `GET /api/v1/orders/{orderId}/payments` - Get payment history

### Admin Endpoints (Admin Role Required)

- `POST /api/v1/admin/products` - Create product
- `PUT /api/v1/admin/products/{id}` - Update product
- `PUT /api/v1/admin/inventory/{id}` - Update inventory
- `GET /api/v1/admin/orders` - View all orders
- `GET /api/v1/admin/users` - Manage users

## 🏗️ Module Architecture

```
com.orderhub/
│
├── common/              # Shared utilities
│   ├── config/          # Spring configuration
│   └── exception/       # Global exception handling
│
├── auth/                # Authentication & Authorization
│   ├── controller/      # AuthController, AdminUserController
│   ├── dto/             # LoginRequest, TokenResponse, etc.
│   ├── entity/          # User, RefreshToken
│   ├── repository/      # UserRepository, RefreshTokenRepository
│   ├── security/        # JwtTokenProvider, SecurityConfig
│   └── service/         # AuthService, UserService
│
├── catalog/             # Product Catalog
│   ├── controller/      # ProductController, AdminProductController
│   ├── dto/             # ProductResponse, CreateProductRequest
│   ├── entity/          # Product
│   ├── repository/      # ProductRepository
│   └── service/         # ProductService
│
├── inventory/           # Stock Management
│   ├── controller/      # AdminInventoryController
│   ├── dto/             # InventoryResponse, UpdateInventoryRequest
│   ├── entity/          # Inventory
│   ├── repository/      # InventoryRepository
│   └── service/         # InventoryService
│
└── orders/              # Order Processing
    ├── controller/      # OrderController, AdminOrderController
    ├── dto/             # OrderResponse, CreateOrderRequest
    ├── entity/          # Order, OrderItem
    ├── repository/      # OrderRepository, OrderItemRepository
    └── service/         # OrderService
```

## 🔐 Security

### Authentication Flow

1. User registers via `/api/v1/auth/register`
2. User logs in via `/api/v1/auth/login` (receives JWT tokens)
3. Client includes `Authorization: Bearer {accessToken}` in requests
4. Access token expires after 15 minutes
5. Refresh token used to get new access token (valid for 7 days)

### Password Security

- BCrypt hashing with strength 10
- Minimum 8 characters required
- Passwords never logged or returned in responses

### Role-Based Access Control

- `ROLE_USER` - Standard customer access
- `ROLE_ADMIN` - Full system access

## 📊 Database Schema

### Key Tables

- **users** - User accounts with roles
- **products** - Product catalog
- **inventory** - Stock levels (optimistic locking)
- **orders** - Customer orders
- **order_items** - Order line items
- **refresh_tokens** - JWT refresh token storage
- **payments** - Payment transactions (future)

### Relationships

```
users (1) ----< (N) orders
products (1) ----< (N) order_items
orders (1) ----< (N) order_items
products (1) --- (1) inventory
```

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify

# Test coverage report
mvn clean verify jacoco:report
# Report: target/site/jacoco/index.html

# Run specific test
mvn test -Dtest=OrderServiceTest
```

## 📈 Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health (requires auth)
curl http://localhost:8080/actuator/health \
  -H "Authorization: Bearer {admin-token}"
```

### Metrics

```bash
# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## 🐛 Troubleshooting

### Common Issues

**Application won't start:**

- Check database is running: `docker compose ps`
- Verify connection: `psql -h localhost -U orderhub -d orderhub`
- Check logs: `docker compose logs backend`

**JWT token issues:**

- Access token expires after 15 min (use refresh token)
- Ensure JWT_SECRET is set correctly
- Check token format: `Bearer {token}`

**Database migration errors:**

- Don't modify existing migrations
- Check Flyway status: `mvn flyway:info`
- In dev, reset database: `docker compose down -v`

**Optimistic locking conflicts:**

- Retry request with fresh data
- Check version field in update requests
- Review concurrent access patterns

See [Deployment Guide](docs/DEPLOYMENT_GUIDE.md#troubleshooting-production-issues) for production troubleshooting.

## 🤝 Contributing

### Code Style

- Follow Spring Boot conventions
- Use DTOs for all API requests/responses
- Never expose entities directly
- Use `@Transactional` on write operations
- Document public APIs with JavaDoc

### Commit Guidelines

```bash
# Format: <type>(<scope>): <description>

feat(auth): add password reset functionality
fix(orders): resolve optimistic locking issue
docs(api): update API reference
test(catalog): add product service tests
refactor(inventory): improve stock decrement logic
```

### Pull Request Process

1. Create feature branch from `main`
2. Write tests for new functionality
3. Ensure all tests pass: `mvn verify`
4. Update documentation if needed
5. Create PR with clear description

## 📝 Version History

See [CHANGELOG.md](../../CHANGELOG.md) for detailed version history.

## 📄 License

Copyright © 2026 OrderHub. All rights reserved.

---

## 💡 Need Help?

- **API Documentation:** http://localhost:8080/swagger-ui.html
- **Quick Reference:** [API_QUICK_REFERENCE.md](docs/API_QUICK_REFERENCE.md)
- **Developer Guide:** [DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)
- **Deployment Guide:** [DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)

😊
