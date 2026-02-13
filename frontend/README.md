# OrderHub Frontend

> 📖 For full project documentation including backend setup, see the [root README](../README.md)

This is the Angular 17+ frontend for OrderHub, built with standalone components, TypeScript 5.x, and Angular Signals.

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm start
# or: ng serve
```

Navigate to **http://localhost:4200**. The application will automatically reload on file changes.

⚠️ **Important**: The backend API must be running at `http://localhost:8080` (configured in `proxy.conf.json`).

See the [Quick Start Guide](./docs/QUICK_START.md) for detailed setup instructions.

## 📚 Documentation

### Essential Guides

- **[Quick Start Guide](./docs/QUICK_START.md)** - Get up and running in 5 minutes
- **[Architecture Guide](./docs/ARCHITECTURE.md)** - System design and architectural patterns
- **[Developer Guide](./docs/DEVELOPER_GUIDE.md)** - Development workflow and best practices
- **[API Integration Guide](./docs/API_INTEGRATION.md)** - Backend API integration patterns
- **[Component Guide](./docs/COMPONENT_GUIDE.md)** - Component architecture and patterns

### Quick Links

- [Project Structure](#project-structure)
- [Available Commands](#available-commands)
- [Tech Stack](#tech-stack)
- [Key Features](#key-features)

## 🏗️ Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/              # Singleton services, guards, interceptors
│   │   │   ├── guards/        # Route guards (auth, admin)
│   │   │   ├── interceptors/  # HTTP interceptors
│   │   │   ├── models/        # TypeScript interfaces/types
│   │   │   └── services/      # Singleton services
│   │   ├── shared/            # Reusable components, pipes
│   │   │   ├── components/    # Shared UI components
│   │   │   └── pipes/         # Custom pipes
│   │   ├── features/          # Feature modules (lazy-loaded)
│   │   │   ├── auth/          # Authentication (login, register)
│   │   │   ├── catalog/       # Product catalog
│   │   │   ├── cart/          # Shopping cart
│   │   │   ├── checkout/      # Checkout process
│   │   │   ├── orders/        # Order management
│   │   │   └── admin/         # Admin panel
│   │   ├── app.component.ts   # Root component
│   │   ├── app.config.ts      # Application providers
│   │   └── app.routes.ts      # Route configuration
│   ├── assets/                # Static assets
│   ├── environments/          # Environment configs
│   └── styles.scss            # Global styles
├── docs/                      # Documentation
├── angular.json               # Angular workspace config
├── package.json               # Dependencies
├── proxy.conf.json            # Dev server API proxy
└── tsconfig.json              # TypeScript config
```

## 💻 Available Commands

| Command                        | Description                               |
| ------------------------------ | ----------------------------------------- |
| `npm start`                    | Start dev server at http://localhost:4200 |
| `npm run build`                | Build for production (output: `dist/`)    |
| `npm test`                     | Run unit tests via Karma                  |
| `npm run watch`                | Build in watch mode                       |
| `ng generate component <name>` | Generate new component                    |
| `ng generate service <name>`   | Generate new service                      |
| `ng serve --port 4300`         | Start on custom port                      |

## 🛠️ Tech Stack

| Technology          | Version | Purpose                   |
| ------------------- | ------- | ------------------------- |
| **Angular**         | 17.3+   | Core framework            |
| **TypeScript**      | 5.4+    | Type-safe development     |
| **RxJS**            | 7.8+    | Reactive programming      |
| **Angular Signals** | 17.3+   | Reactive state management |
| **SCSS**            | -       | Styling                   |
| **Karma + Jasmine** | 5.1+    | Unit testing              |

### Key Angular Features Used

- ✅ **Standalone Components** - No NgModules, simplified architecture
- ✅ **Angular Signals** - Modern reactive state management
- ✅ **Functional Guards** - `CanActivateFn` for route protection
- ✅ **Functional Interceptors** - `HttpInterceptorFn` for HTTP handling
- ✅ **Lazy Loading** - On-demand feature loading
- ✅ **Reactive Forms** - Type-safe form handling

## ✨ Key Features

### User Features

- **Product Catalog**: Browse products with pagination, filtering, and sorting
- **Product Details**: View detailed product information and inventory
- **Shopping Cart**: Add, remove, and update cart items
- **Checkout**: Complete order placement and payment
- **Order History**: View past orders and their status
- **Authentication**: Secure login and registration

### Admin Features

- **Product Management**: Create, update, and delete products
- **Inventory Management**: Adjust stock levels
- **Order Management**: View and manage all orders
- **User Management**: Manage user accounts and roles

### Technical Features

- **JWT Authentication**: Secure token-based auth
- **Role-Based Access Control**: User and Admin roles
- **API Proxy**: Seamless backend integration
- **Error Handling**: User-friendly error messages
- **Loading States**: Proper loading indicators
- **Optimistic UI Updates**: Instant feedback on actions

## 🔧 Development

### Prerequisites

- Node.js 18.x or higher
- npm 9.x or higher
- Angular CLI 17.x (optional): `npm install -g @angular/cli`

### First-Time Setup

```bash
# Install dependencies
npm install

# Verify installation
ng version

# Start backend (required)
cd ../backend
mvn spring-boot:run

# In new terminal, start frontend
cd ../frontend
npm start
```

### Development Workflow

1. **Create Feature Branch**: `git checkout -b feature/my-feature`
2. **Make Changes**: Edit code with hot reload
3. **Test**: `npm test`
4. **Build**: `npm run build` (verify no errors)
5. **Commit**: `git commit -m "feat: add new feature"`
6. **Push**: `git push origin feature/my-feature`

See the [Developer Guide](./docs/DEVELOPER_GUIDE.md) for detailed workflows.

## 🧪 Testing

```bash
# Run unit tests
npm test

# Run tests in headless mode (CI)
ng test --watch=false --browsers=ChromeHeadless

# Run tests with coverage
ng test --code-coverage

# View coverage report
open coverage/index.html
```

## 🚢 Production Build

```bash
# Build for production
npm run build

# Output is in dist/frontend/
# Serve with a web server (e.g., Nginx)
```

### Build Optimization

Production builds include:

- ✅ Minification
- ✅ Tree-shaking
- ✅ Dead code elimination
- ✅ Lazy loading
- ✅ Ahead-of-Time (AOT) compilation

## 🐛 Troubleshooting

### Common Issues

**Port 4200 already in use**

```bash
lsof -ti:4200 | xargs kill -9
# or use different port
ng serve --port 4300
```

**API connection errors**

```bash
# Verify backend is running
curl http://localhost:8080/api/v1/health

# Check proxy.conf.json configuration
```

**Module not found errors**

```bash
rm -rf node_modules package-lock.json
npm install
```

See the [Developer Guide](./docs/DEVELOPER_GUIDE.md#troubleshooting) for more solutions.

## 📖 Architecture

### Design Patterns

- **Smart/Presentational Components**: Clear separation of concerns
- **Service Layer**: Centralized API communication
- **Signals for State**: Modern reactive state management
- **Observables for Streams**: RxJS for async operations
- **Guard-Based Security**: Route protection with functional guards

See the [Architecture Guide](./docs/ARCHITECTURE.md) for detailed patterns.

## 🔌 API Integration

The frontend communicates with the Spring Boot backend via REST API:

- **Base URL**: `/api/v1/*` (proxied to `http://localhost:8080`)
- **Authentication**: JWT Bearer tokens
- **Error Format**: RFC 7807 Problem Details
- **Pagination**: Page-based with metadata

See the [API Integration Guide](./docs/API_INTEGRATION.md) for details.

## 🤝 Contributing

1. Follow the [Developer Guide](./docs/DEVELOPER_GUIDE.md)
2. Use conventional commits: `feat:`, `fix:`, `docs:`, etc.
3. Write tests for new features
4. Update documentation as needed

## 📝 License

This project is part of the OrderHub MVP portfolio project.

## 🔗 Related Documentation

- [Backend Documentation](../backend/docs/)
- [Project PRD](../docs/prd.md)
- [Implementation Plans](../docs/plans/)
- [OrderHub MVP Overview](../docs/OrderHub_MVP.md)

---

**Need help?** Check the [Quick Start Guide](./docs/QUICK_START.md) or [Developer Guide](./docs/DEVELOPER_GUIDE.md).

**Happy coding! 😊**
