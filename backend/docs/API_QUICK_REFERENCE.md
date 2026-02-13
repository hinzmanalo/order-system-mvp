# API Quick Reference

Fast reference guide for OrderHub backend APIs.

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

### Register

```bash
POST /auth/register
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
# → 201 Created + UserResponse
```

### Login

```bash
POST /auth/login
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
# → 200 OK + TokenResponse (accessToken, refreshToken)
```

### Refresh Token

```bash
POST /auth/refresh
{
  "refreshToken": "your-refresh-token"
}
# → 200 OK + TokenResponse
```

### Get Current User

```bash
GET /auth/me
Authorization: Bearer {accessToken}
# → 200 OK + UserResponse
```

### Logout

```bash
POST /auth/logout
Authorization: Bearer {accessToken}
# → 204 No Content
```

## Products (Public)

### List Products

```bash
GET /products?page=0&size=20&sort=name,asc
# Optional: &name=headphones&minPrice=50&maxPrice=500
# → 200 OK + Page<ProductResponse>
```

### Get Product

```bash
GET /products/{productId}
# → 200 OK + ProductResponse
```

## Orders (Authenticated)

### Create Order

```bash
POST /orders
Authorization: Bearer {accessToken}
{
  "items": [
    { "productId": "uuid", "quantity": 2 }
  ]
}
# → 201 Created + OrderResponse
```

### List My Orders

```bash
GET /orders?page=0&size=10
Authorization: Bearer {accessToken}
# Optional: &status=CONFIRMED&startDate=2026-01-01&endDate=2026-12-31
# → 200 OK + Page<OrderResponse>
```

### Get Order

```bash
GET /orders/{orderId}
Authorization: Bearer {accessToken}
# → 200 OK + OrderResponse
```

### Cancel Order

```bash
POST /orders/{orderId}/cancel
Authorization: Bearer {accessToken}
# → 200 OK + OrderResponse (status: CANCELLED)
```

## Admin - Users

### List All Users

```bash
GET /admin/users?page=0&size=20
Authorization: Bearer {adminToken}
# → 200 OK + Page<UserResponse>
```

### Get User

```bash
GET /admin/users/{userId}
Authorization: Bearer {adminToken}
# → 200 OK + UserResponse
```

### Update User Role

```bash
PUT /admin/users/{userId}/role
Authorization: Bearer {adminToken}
{
  "role": "ROLE_ADMIN"
}
# → 200 OK + UserResponse
```

## Admin - Products

### Create Product

```bash
POST /admin/products
Authorization: Bearer {adminToken}
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "price": 29.99,
  "sku": "MOUSE-001",
  "initialStock": 100
}
# → 201 Created + ProductResponse
```

### Update Product

```bash
PUT /admin/products/{productId}
Authorization: Bearer {adminToken}
{
  "name": "Updated Name",
  "description": "Updated description",
  "price": 34.99,
  "active": true
}
# → 200 OK + ProductResponse
```

### Delete Product

```bash
DELETE /admin/products/{productId}
Authorization: Bearer {adminToken}
# → 204 No Content
```

## Admin - Inventory

### Get Inventory

```bash
GET /admin/inventory/{productId}
Authorization: Bearer {adminToken}
# → 200 OK + InventoryResponse
```

### Update Inventory

```bash
PUT /admin/inventory/{productId}
Authorization: Bearer {adminToken}
{
  "quantity": 150,
  "version": 5
}
# → 200 OK + InventoryResponse
```

### Adjust Inventory

```bash
POST /admin/inventory/{productId}/adjust
Authorization: Bearer {adminToken}
{
  "adjustment": 50,
  "reason": "RESTOCK"
}
# → 200 OK + InventoryResponse
```

## Admin - Orders

### List All Orders

```bash
GET /admin/orders?page=0&size=20
Authorization: Bearer {adminToken}
# Optional: &status=CONFIRMED&userId={uuid}
# → 200 OK + Page<OrderResponse>
```

### Get Order

```bash
GET /admin/orders/{orderId}
Authorization: Bearer {adminToken}
# → 200 OK + OrderResponse
```

### Update Order Status

```bash
PUT /admin/orders/{orderId}/status
Authorization: Bearer {adminToken}
{
  "status": "PAID"
}
# → 200 OK + OrderResponse
```

## Common Response Codes

| Code | Meaning                                   |
| ---- | ----------------------------------------- |
| 200  | Success                                   |
| 201  | Created                                   |
| 204  | No Content (success, no body)             |
| 400  | Bad Request (validation error)            |
| 401  | Unauthorized (missing/invalid token)      |
| 403  | Forbidden (insufficient permissions)      |
| 404  | Not Found                                 |
| 409  | Conflict (optimistic lock, business rule) |
| 500  | Internal Server Error                     |

## Error Response Format

```json
{
  "type": "https://orderhub.com/errors/error-type",
  "title": "Error Title",
  "status": 400,
  "detail": "Detailed error message",
  "instance": "/api/v1/endpoint",
  "timestamp": "2026-02-13T11:30:00Z"
}
```

## Testing with cURL

### Complete Flow Example

```bash
# 1. Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "Test123!"
  }'

# 2. Login
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }' | jq -r '.accessToken')

# 3. Browse products
curl http://localhost:8080/api/v1/products?size=5

# 4. Create order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "product-uuid-here",
        "quantity": 2
      }
    ]
  }'

# 5. Get my orders
curl http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN"
```

## Pagination Parameters

| Parameter | Type    | Default | Description                            |
| --------- | ------- | ------- | -------------------------------------- |
| `page`    | integer | 0       | Page number (0-indexed)                |
| `size`    | integer | 20      | Items per page (max 100)               |
| `sort`    | string  | varies  | `field,direction` (e.g., `price,desc`) |

**Example:**

```bash
GET /products?page=2&size=50&sort=price,asc&sort=name,desc
```

## Swagger UI

Interactive API testing:

```
http://localhost:8080/swagger-ui.html
```

😊
