# Developer Guide

Guide for common development tasks in OrderHub backend.

## Table of Contents

- [Adding a New Endpoint](#adding-a-new-endpoint)
- [Creating Database Migrations](#creating-database-migrations)
- [Adding Validation Rules](#adding-validation-rules)
- [Implementing Business Logic](#implementing-business-logic)
- [Writing Tests](#writing-tests)
- [Exception Handling](#exception-handling)
- [Logging Best Practices](#logging-best-practices)
- [Optimistic Locking](#optimistic-locking)

---

## Adding a New Endpoint

### Step 1: Create DTO Classes

**Request DTO:**
```java
package com.orderhub.module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new product.
 */
@Data
public class CreateProductRequest {
    
    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @Positive(message = "Initial stock must be positive")
    private Integer initialStock = 0;
}
```

**Response DTO:**
```java
package com.orderhub.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for product information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private Boolean active;
    private Integer availableStock;
    private LocalDateTime createdAt;
}
```

### Step 2: Add Service Method

**Service Interface:**
```java
package com.orderhub.module.service;

import com.orderhub.module.dto.CreateProductRequest;
import com.orderhub.module.dto.ProductResponse;

import java.util.UUID;

public interface ProductService {
    /**
     * Creates a new product with initial inventory.
     *
     * @param request the product creation request
     * @return the created product
     */
    ProductResponse createProduct(CreateProductRequest request);
}
```

**Service Implementation:**
```java
package com.orderhub.module.service;

import com.orderhub.common.exception.DuplicateResourceException;
import com.orderhub.module.dto.CreateProductRequest;
import com.orderhub.module.dto.ProductResponse;
import com.orderhub.module.entity.Product;
import com.orderhub.module.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);
    
    private final ProductRepository productRepository;
    
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        logger.info("Creating product with SKU: {}", request.getSku());
        
        // Validate SKU uniqueness
        if (productRepository.existsBySku(request.getSku())) {
            logger.warn("Attempt to create product with duplicate SKU: {}", request.getSku());
            throw new DuplicateResourceException("Product with SKU " + request.getSku() + " already exists");
        }
        
        // Create entity
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setActive(true);
        
        // Save to database
        Product saved = productRepository.save(product);
        
        logger.info("Product created successfully with ID: {}", saved.getId());
        
        // Map to response DTO
        return mapToResponse(saved);
    }
    
    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setSku(product.getSku());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
```

### Step 3: Add Controller Endpoint

```java
package com.orderhub.module.controller;

import com.orderhub.module.dto.CreateProductRequest;
import com.orderhub.module.dto.ProductResponse;
import com.orderhub.module.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for product management.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@Tag(name = "Admin - Products", description = "Product management endpoints")
public class AdminProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminProductController.class);
    
    private final ProductService productService;
    
    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Creates a new product with initial inventory")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Product with SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        logger.info("POST /api/v1/admin/products - Creating product: {}", request.getName());
        
        ProductResponse response = productService.createProduct(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## Creating Database Migrations

### Step 1: Create Migration File

Create file in `src/main/resources/db/migration/`:

```
V8__add_product_category.sql
```

**Naming convention:** `V{version}__{description}.sql`
- Version must be incremental (V1, V2, V3...)
- Double underscore before description
- Use snake_case for description

### Step 2: Write SQL DDL

```sql
-- V8__add_product_category.sql

-- Add category table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add category_id to products
ALTER TABLE products
ADD COLUMN category_id UUID,
ADD CONSTRAINT fk_products_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id);

-- Create index for category lookups
CREATE INDEX idx_products_category_id ON products(category_id);

-- Insert default category
INSERT INTO categories (name, description) VALUES
('General', 'Default product category');

-- Update existing products to use default category
UPDATE products
SET category_id = (SELECT id FROM categories WHERE name = 'General')
WHERE category_id IS NULL;
```

### Step 3: Test Migration

```bash
# Restart application - Flyway auto-applies
mvn spring-boot:run

# Or apply manually
mvn flyway:migrate

# Check migration status
mvn flyway:info
```

### Migration Best Practices

✅ **DO:**
- Always include rollback plan
- Test migrations on copy of production data
- Use transactions for data migrations
- Add indexes for new foreign keys
- Document complex migrations

❌ **DON'T:**
- Never modify applied migrations
- Don't use database-specific syntax without fallback
- Avoid massive data migrations in DDL scripts

---

## Adding Validation Rules

### Built-in Validators

```java
import jakarta.validation.constraints.*;

public class ExampleRequest {
    @NotNull(message = "Field cannot be null")
    private String field1;
    
    @NotBlank(message = "Field cannot be empty")
    private String field2;
    
    @Email(message = "Must be valid email")
    private String email;
    
    @Size(min = 8, max = 100, message = "Must be between 8 and 100 characters")
    private String password;
    
    @Positive(message = "Must be positive number")
    private BigDecimal price;
    
    @Min(value = 0, message = "Minimum value is 0")
    @Max(value = 100, message = "Maximum value is 100")
    private Integer quantity;
    
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, numbers, and hyphens")
    private String sku;
    
    @Past(message = "Date must be in the past")
    private LocalDate birthDate;
    
    @Future(message = "Date must be in the future")
    private LocalDateTime deliveryDate;
}
```

### Custom Validator

**Step 1: Create Annotation**
```java
package com.orderhub.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
@Documented
public @interface UniqueEmail {
    String message() default "Email already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Step 2: Implement Validator**
```java
package com.orderhub.common.validation;

import com.orderhub.auth.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {
    
    private final UserRepository userRepository;
    
    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) {
            return true; // Use @NotNull for null checks
        }
        return !userRepository.existsByEmail(email);
    }
}
```

**Step 3: Use Annotation**
```java
public class RegisterRequest {
    @UniqueEmail
    @Email
    @NotBlank
    private String email;
}
```

---

## Implementing Business Logic

### Service Layer Pattern

```java
@Service
public class OrderServiceImpl implements OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ProductService productService;
    
    // Constructor injection (recommended)
    public OrderServiceImpl(
            OrderRepository orderRepository,
            InventoryService inventoryService,
            ProductService productService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.productService = productService;
    }
    
    @Override
    @Transactional // CRITICAL: Ensures atomicity
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        logger.info("Creating order for user {} with {} items", userId, request.getItems().size());
        
        // 1. Validate all products exist and are active
        validateProducts(request);
        
        // 2. Check and reserve inventory
        reserveInventory(request);
        
        // 3. Create order
        Order order = buildOrder(userId, request);
        Order saved = orderRepository.save(order);
        
        logger.info("Order created successfully: {}", saved.getId());
        
        return mapToResponse(saved);
    }
    
    private void validateProducts(CreateOrderRequest request) {
        for (OrderItemRequest item : request.getItems()) {
            ProductResponse product = productService.getProductById(item.getProductId());
            
            if (!product.getActive()) {
                throw new BusinessRuleException("Product " + product.getName() + " is not active");
            }
        }
    }
    
    private void reserveInventory(CreateOrderRequest request) {
        for (OrderItemRequest item : request.getItems()) {
            try {
                inventoryService.decrementStock(item.getProductId(), item.getQuantity());
            } catch (InsufficientStockException e) {
                // Business-friendly error message
                throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Requested: %d, Available: %d",
                        e.getProductId(), item.getQuantity(), e.getAvailableStock())
                );
            }
        }
    }
}
```

### Transaction Management

**Use `@Transactional` for:**
- Operations that modify database
- Multiple database operations that must succeed/fail together
- Operations requiring read-write consistency

**Don't use `@Transactional` for:**
- Read-only operations (use `@Transactional(readOnly = true)` instead)
- Operations with external API calls (transaction timeout)

---

## Writing Tests

### Unit Test

```java
package com.orderhub.module.service;

import com.orderhub.module.dto.ProductResponse;
import com.orderhub.module.entity.Product;
import com.orderhub.module.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private ProductServiceImpl productService;
    
    @Test
    void createProduct_Success() {
        // Given
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSku("TEST-001");
        request.setPrice(BigDecimal.valueOf(99.99));
        
        Product savedProduct = new Product();
        savedProduct.setId(UUID.randomUUID());
        savedProduct.setName(request.getName());
        savedProduct.setSku(request.getSku());
        
        when(productRepository.existsBySku(request.getSku())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        
        // When
        ProductResponse response = productService.createProduct(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }
    
    @Test
    void createProduct_DuplicateSku_ThrowsException() {
        // Given
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("DUPLICATE-SKU");
        
        when(productRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> productService.createProduct(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("already exists");
        
        verify(productRepository, never()).save(any());
    }
}
```

### Integration Test

```java
package com.orderhub.module.controller;

import com.orderhub.module.dto.CreateProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Rollback after each test
class AdminProductControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_Success() throws Exception {
        String requestJson = """
            {
                "name": "Integration Test Product",
                "description": "Test description",
                "price": 99.99,
                "sku": "INT-TEST-001",
                "initialStock": 50
            }
            """;
        
        mockMvc.perform(post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Integration Test Product"))
            .andExpect(jsonPath("$.sku").value("INT-TEST-001"));
    }
    
    @Test
    @WithMockUser(roles = "USER") // Wrong role
    void createProduct_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

---

## Exception Handling

### Custom Exception

```java
package com.orderhub.common.exception;

/**
 * Exception thrown when insufficient inventory is available.
 */
public class InsufficientStockException extends RuntimeException {
    
    private final UUID productId;
    private final Integer requested;
    private final Integer available;
    
    public InsufficientStockException(UUID productId, Integer requested, Integer available) {
        super(String.format("Insufficient stock for product %s. Requested: %d, Available: %d",
            productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }
    
    // Getters
    public UUID getProductId() { return productId; }
    public Integer getRequested() { return requested; }
    public Integer getAvailable() { return available; }
}
```

### Global Exception Handler

```java
package com.orderhub.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        logger.warn("Insufficient stock: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setTitle("Insufficient Stock");
        problem.setType(URI.create("https://orderhub.com/errors/insufficient-stock"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("productId", ex.getProductId());
        problem.setProperty("requested", ex.getRequested());
        problem.setProperty("available", ex.getAvailable());
        
        return problem;
    }
}
```

---

## Logging Best Practices

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
}
```

### Log Levels

```java
// TRACE - Very detailed, development only
logger.trace("Entering method with params: {}, {}", param1, param2);

// DEBUG - Debugging information
logger.debug("Processing order {} with {} items", orderId, itemCount);

// INFO - Important business events
logger.info("Order {} created successfully for user {}", orderId, userId);

// WARN - Potentially harmful situations
logger.warn("Product {} stock is low: {} units remaining", productId, stock);

// ERROR - Error events (with exception)
logger.error("Failed to process payment for order {}", orderId, exception);
```

### Best Practices

✅ **DO:**
```java
// Use parameterized logging
logger.info("User {} logged in at {}", userId, timestamp);

// Log exceptions with full stack trace
logger.error("Payment failed for order {}", orderId, exception);

// Include context
logger.info("Order created: orderId={}, userId={}, total={}", orderId, userId, total);
```

❌ **DON'T:**
```java
// String concatenation (performance hit)
logger.info("User " + userId + " logged in"); // BAD

// Log sensitive data
logger.info("User password: {}", password); // NEVER

// Redundant logging
logger.info("Order created"); // Not enough context
```

---

## Optimistic Locking

### Entity Setup

```java
@Entity
@Table(name = "inventory")
public class Inventory {
    
    @Id
    private UUID productId;
    
    private Integer quantity;
    
    @Version // Enables optimistic locking
    private Long version;
    
    // Other fields...
}
```

### Update with Version Check

```java
@Service
public class InventoryServiceImpl implements InventoryService {
    
    @Transactional
    public InventoryResponse updateQuantity(UUID productId, Integer newQuantity, Long version) {
        Inventory inventory = inventoryRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        
        // Version mismatch throws OptimisticLockException
        if (!inventory.getVersion().equals(version)) {
            throw new OptimisticLockException("Inventory was modified by another transaction");
        }
        
        inventory.setQuantity(newQuantity);
        Inventory saved = inventoryRepository.save(inventory);
        
        return mapToResponse(saved);
    }
}
```

### Client Retry Pattern

```java
// Client should handle 409 Conflict and retry
public void updateInventoryWithRetry(UUID productId, Integer quantity) {
    int maxRetries = 3;
    int attempt = 0;
    
    while (attempt < maxRetries) {
        try {
            // 1. Get current state
            InventoryResponse current = inventoryService.getInventory(productId);
            
            // 2. Update with version
            inventoryService.updateQuantity(productId, quantity, current.getVersion());
            
            return; // Success
            
        } catch (OptimisticLockException e) {
            attempt++;
            if (attempt >= maxRetries) {
                throw new RuntimeException("Failed to update inventory after " + maxRetries + " attempts", e);
            }
            // Brief delay before retry
            Thread.sleep(100);
        }
    }
}
```

---

## Tips & Tricks

### Entity to DTO Mapping

Consider using ModelMapper or MapStruct for complex mappings:

```java
// Manual mapping (simple cases)
private ProductResponse mapToResponse(Product product) {
    ProductResponse response = new ProductResponse();
    response.setId(product.getId());
    response.setName(product.getName());
    // ... more fields
    return response;
}

// Or use Java Records (Java 17+)
public record ProductResponse(
    UUID id,
    String name,
    BigDecimal price
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }
}
```

### Repository Query Methods

```java
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // Spring generates query from method name
    boolean existsBySku(String sku);
    
    Optional<Product> findBySku(String sku);
    
    List<Product> findByActiveTrue();
    
    // Custom JPQL query
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.price BETWEEN :min AND :max")
    List<Product> findActiveProductsInPriceRange(
        @Param("min") BigDecimal minPrice,
        @Param("max") BigDecimal maxPrice
    );
    
    // Native SQL query
    @Query(value = "SELECT * FROM products WHERE name ILIKE %:search%", nativeQuery = true)
    List<Product> searchByName(@Param("search") String searchTerm);
}
```

---

**Ready to build!** Refer to [README.md](../README.md) for setup and [API_QUICK_REFERENCE.md](API_QUICK_REFERENCE.md) for endpoints 😊
