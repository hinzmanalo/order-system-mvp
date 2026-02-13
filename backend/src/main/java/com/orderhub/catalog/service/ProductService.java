package com.orderhub.catalog.service;

import com.orderhub.catalog.dto.ProductRequest;
import com.orderhub.catalog.dto.ProductResponse;
import com.orderhub.catalog.dto.ProductStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface for managing product catalog operations.
 * <p>
 * Provides methods for browsing active products, viewing product details,
 * and admin operations for creating, updating, and managing product status.
 * </p>
 *
 * @since 1.0.0
 */
public interface ProductService {

    /**
     * Retrieves a paginated list of active products with optional filters.
     * <p>
     * Supports filtering by name (case-insensitive substring match) and price
     * range.
     * All filter parameters are optional.
     * </p>
     *
     * @param name     optional name filter
     * @param minPrice optional minimum price filter (inclusive)
     * @param maxPrice optional maximum price filter (inclusive)
     * @param pageable pagination and sorting parameters
     * @return page of active products matching the filter criteria
     */
    Page<ProductResponse> getActiveProducts(String name, BigDecimal minPrice,
            BigDecimal maxPrice, Pageable pageable);

    /**
     * Retrieves a single product by ID.
     *
     * @param id the product ID
     * @return the product details
     * @throws com.orderhub.common.exception.ResourceNotFoundException if product
     *                                                                 not found
     */
    ProductResponse getProductById(UUID id);

    /**
     * Creates a new product.
     * <p>
     * Also creates an associated inventory record with quantity 0.
     * </p>
     *
     * @param request the product creation request
     * @return the created product
     * @throws com.orderhub.common.exception.DuplicateResourceException if SKU
     *                                                                  already
     *                                                                  exists
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Updates an existing product.
     * <p>
     * Validates that SKU is unique (excluding the product being updated).
     * </p>
     *
     * @param id      the product ID
     * @param request the product update request
     * @return the updated product
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if product
     *                                                                  not found
     * @throws com.orderhub.common.exception.DuplicateResourceException if SKU
     *                                                                  already
     *                                                                  exists
     */
    ProductResponse updateProduct(UUID id, ProductRequest request);

    /**
     * Updates a product's active status.
     * <p>
     * Used to activate or deactivate products. Deactivated products
     * will not appear in public product listings.
     * </p>
     *
     * @param id      the product ID
     * @param request the status update request
     * @return the updated product
     * @throws com.orderhub.common.exception.ResourceNotFoundException if product
     *                                                                 not found
     */
    ProductResponse updateProductStatus(UUID id, ProductStatusRequest request);
}
