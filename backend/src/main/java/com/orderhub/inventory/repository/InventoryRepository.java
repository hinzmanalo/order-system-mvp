package com.orderhub.inventory.repository;

import com.orderhub.inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing Inventory entities.
 * <p>
 * Provides CRUD operations and custom queries for inventory management,
 * including paginated listing with product details and product-based lookups.
 * </p>
 *
 * @since 1.0.0
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Finds inventory record by product ID.
     *
     * @param productId the product ID to search for
     * @return Optional containing the inventory record if found
     */
    Optional<Inventory> findByProductId(UUID productId);

    /**
     * Retrieves paginated inventory list with product details eagerly fetched.
     * This query joins with the product table to avoid N+1 query issues.
     *
     * @param pageable pagination parameters
     * @return page of inventory records with product details loaded
     */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.product")
    Page<Inventory> findAllWithProduct(Pageable pageable);
}
