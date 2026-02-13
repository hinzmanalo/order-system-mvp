package com.orderhub.catalog.controller;

import com.orderhub.catalog.dto.ProductResponse;
import com.orderhub.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public REST controller for browsing the product catalog.
 * <p>
 * Provides endpoints for customers to view active products with filtering,
 * sorting, and pagination. No authentication required.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Public product catalog endpoints")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Retrieves a paginated list of active products with optional filters.
     * <p>
     * Supports filtering by product name (case-insensitive) and price range.
     * Results can be sorted by any product field.
     * </p>
     *
     * @param name     optional name filter (case-insensitive substring match)
     * @param minPrice optional minimum price filter (inclusive)
     * @param maxPrice optional maximum price filter (inclusive)
     * @param pageable pagination and sorting parameters (default: page 0, size 20)
     * @return paginated list of active products
     */
    @Operation(summary = "Browse active products", description = "Retrieves a paginated list of active products with optional name and price filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getActiveProducts(
            @Parameter(description = "Filter by product name (case-insensitive)") @RequestParam(required = false) String name,

            @Parameter(description = "Filter by minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Filter by maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Pagination parameters (page, size, sort)") @PageableDefault(size = 20) Pageable pageable) {

        logger.debug("GET /api/v1/products - name: {}, minPrice: {}, maxPrice: {}, page: {}",
                name, minPrice, maxPrice, pageable.getPageNumber());

        Page<ProductResponse> products = productService.getActiveProducts(
                name, minPrice, maxPrice, pageable);

        logger.info("Returning {} products (page {} of {})",
                products.getNumberOfElements(), products.getNumber() + 1, products.getTotalPages());

        return ResponseEntity.ok(products);
    }

    /**
     * Retrieves detailed information for a single product.
     *
     * @param id the product ID
     * @return the product details
     */
    @Operation(summary = "Get product by ID", description = "Retrieves detailed information for a specific product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID", required = true) @PathVariable UUID id) {

        logger.debug("GET /api/v1/products/{}", id);

        ProductResponse product = productService.getProductById(id);

        logger.info("Returning product: {}", product.getName());

        return ResponseEntity.ok(product);
    }
}
