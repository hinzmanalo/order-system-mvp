package com.orderhub.catalog.controller;

import com.orderhub.catalog.dto.ProductRequest;
import com.orderhub.catalog.dto.ProductResponse;
import com.orderhub.catalog.dto.ProductStatusRequest;
import com.orderhub.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin REST controller for managing products.
 * <p>
 * Provides endpoints for admins to create, update, and manage product status.
 * All endpoints require ADMIN role.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Products", description = "Admin endpoints for product management")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    private static final Logger logger = LoggerFactory.getLogger(AdminProductController.class);

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Creates a new product.
     * <p>
     * SKU must be unique across all products. Upon creation, an inventory
     * record is automatically created with quantity 0.
     * </p>
     *
     * @param request the product creation request
     * @return the created product
     */
    @Operation(summary = "Create product", description = "Creates a new product with associated inventory record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product details", required = true) @Valid @RequestBody ProductRequest request) {

        logger.info("POST /api/v1/admin/products - creating product with SKU: {}", request.getSku());

        ProductResponse product = productService.createProduct(request);

        logger.info("Product created successfully with ID: {}", product.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Updates an existing product.
     * <p>
     * All fields from the request will update the product. SKU must remain
     * unique across all products (excluding the product being updated).
     * </p>
     *
     * @param id      the product ID
     * @param request the product update request
     * @return the updated product
     */
    @Operation(summary = "Update product", description = "Updates an existing product's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU or optimistic locking conflict", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id,

            @Parameter(description = "Updated product details", required = true) @Valid @RequestBody ProductRequest request) {

        logger.info("PUT /api/v1/admin/products/{} - updating product", id);

        ProductResponse product = productService.updateProduct(id, request);

        logger.info("Product updated successfully: {}", product.getName());

        return ResponseEntity.ok(product);
    }

    /**
     * Updates a product's active status.
     * <p>
     * Deactivating a product removes it from public listings but preserves
     * it for historical order data.
     * </p>
     *
     * @param id      the product ID
     * @param request the status update request
     * @return the updated product
     */
    @Operation(summary = "Update product status", description = "Activates or deactivates a product (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product status updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - requires ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id,

            @Parameter(description = "Status update", required = true) @Valid @RequestBody ProductStatusRequest request) {

        logger.info("PATCH /api/v1/admin/products/{}/status - setting active to {}",
                id, request.getActive());

        ProductResponse product = productService.updateProductStatus(id, request);

        logger.info("Product status updated: {} is now {}",
                product.getName(), product.isActive() ? "active" : "inactive");

        return ResponseEntity.ok(product);
    }
}
