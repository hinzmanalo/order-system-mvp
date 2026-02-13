package com.orderhub.catalog.repository;

import com.orderhub.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Repository for accessing and managing Product entities.
 * <p>
 * Provides CRUD operations plus custom queries for SKU uniqueness validation
 * and filtered product searches with price range and name filtering.
 * </p>
 *
 * @since 1.0.0
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

        /**
         * Checks if a product with the given SKU exists.
         *
         * @param sku the SKU to check
         * @return true if a product with this SKU exists, false otherwise
         */
        boolean existsBySku(String sku);

        /**
         * Checks if a product with the given SKU exists, excluding a specific product
         * ID.
         * <p>
         * Used during updates to ensure SKU uniqueness while allowing the product
         * being updated to keep its own SKU.
         * </p>
         *
         * @param sku the SKU to check
         * @param id  the product ID to exclude from the check
         * @return true if another product with this SKU exists, false otherwise
         */
        boolean existsBySkuAndIdNot(String sku, UUID id);

        /**
         * Searches for active products with optional filters.
         * <p>
         * Supports filtering by:
         * - Product name (case-insensitive partial match)
         * - Minimum price
         * - Maximum price
         * </p>
         * <p>
         * All filter parameters are optional. Passing null for any parameter
         * will skip that filter.
         * </p>
         *
         * @param name     optional name filter (case-insensitive substring match)
         * @param minPrice optional minimum price filter (inclusive)
         * @param maxPrice optional maximum price filter (inclusive)
         * @param pageable pagination and sorting parameters
         * @return page of products matching the filter criteria
         */
        @Query("SELECT p FROM Product p WHERE p.active = true " +
                        "AND (CAST(:name AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) "
                        +
                        "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
        Page<Product> findActiveProducts(
                        @Param("name") String name,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);
}
