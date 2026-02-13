package com.orderhub.inventory.service;

import com.orderhub.inventory.dto.AdjustStockRequest;
import com.orderhub.inventory.dto.InventoryResponse;
import com.orderhub.inventory.dto.SetStockRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for inventory management operations.
 * <p>
 * Provides both admin operations (set/adjust stock) and internal operations
 * used by the orders module (decrement/restore stock).
 * </p>
 *
 * @since 1.0.0
 */
public interface InventoryService {

    /**
     * Retrieves paginated list of all inventory records with product details.
     *
     * @param pageable pagination parameters
     * @return page of inventory responses
     */
    Page<InventoryResponse> getAllInventory(Pageable pageable);

    /**
     * Retrieves inventory information for a specific product.
     *
     * @param productId the product ID
     * @return inventory response
     * @throws com.orderhub.common.exception.ResourceNotFoundException if inventory
     *                                                                 not found
     */
    InventoryResponse getInventoryByProductId(UUID productId);

    /**
     * Sets absolute stock quantity for a product.
     * This replaces the current quantity with the specified value.
     *
     * @param productId the product ID
     * @param request   the set stock request
     * @return updated inventory response
     * @throws com.orderhub.common.exception.ResourceNotFoundException if inventory
     *                                                                 not found
     */
    InventoryResponse setStock(UUID productId, SetStockRequest request);

    /**
     * Adjusts stock by a relative amount (positive or negative).
     * The resulting quantity must not go below zero.
     *
     * @param productId the product ID
     * @param request   the adjustment request
     * @return updated inventory response
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if inventory
     *                                                                  not found
     * @throws com.orderhub.common.exception.InsufficientStockException if
     *                                                                  adjustment
     *                                                                  would result
     *                                                                  in negative
     *                                                                  stock
     */
    InventoryResponse adjustStock(UUID productId, AdjustStockRequest request);

    /**
     * Decrements stock for order fulfillment.
     * <p>
     * This method is designed to be called within a transaction by the
     * OrderService.
     * It does not have its own @Transactional annotation to participate in the
     * caller's transaction.
     * </p>
     *
     * @param productId the product ID
     * @param quantity  the quantity to decrement
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if inventory
     *                                                                  not found
     * @throws com.orderhub.common.exception.InsufficientStockException if
     *                                                                  insufficient
     *                                                                  stock
     *                                                                  available
     */
    void decrementStock(UUID productId, int quantity);

    /**
     * Restores stock when an order is cancelled.
     * <p>
     * This method is designed to be called within a transaction by the
     * OrderService.
     * It does not have its own @Transactional annotation to participate in the
     * caller's transaction.
     * </p>
     *
     * @param productId the product ID
     * @param quantity  the quantity to restore
     * @throws com.orderhub.common.exception.ResourceNotFoundException if inventory
     *                                                                 not found
     */
    void restoreStock(UUID productId, int quantity);
}
