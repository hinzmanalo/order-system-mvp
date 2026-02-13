package com.orderhub.inventory.service;

import com.orderhub.catalog.entity.Product;
import com.orderhub.common.exception.InsufficientStockException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.inventory.dto.AdjustStockRequest;
import com.orderhub.inventory.dto.InventoryResponse;
import com.orderhub.inventory.dto.SetStockRequest;
import com.orderhub.inventory.entity.Inventory;
import com.orderhub.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of inventory management service.
 * <p>
 * Handles stock management with optimistic locking for concurrent safety.
 * Admin operations (set/adjust) are transactional, while internal operations
 * (decrement/restore) participate in the caller's transaction.
 * </p>
 *
 * @since 1.0.0
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        logger.debug("Fetching inventory list, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Inventory> inventoryPage = inventoryRepository.findAllWithProduct(pageable);

        logger.info("Retrieved {} inventory records (page {} of {})",
                inventoryPage.getNumberOfElements(),
                inventoryPage.getNumber() + 1,
                inventoryPage.getTotalPages());

        return inventoryPage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(UUID productId) {
        logger.debug("Fetching inventory for product ID: {}", productId);

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        logger.info("Retrieved inventory for product: {} (SKU: {}), quantity: {}",
                inventory.getProduct().getName(),
                inventory.getProduct().getSku(),
                inventory.getQuantity());

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse setStock(UUID productId, SetStockRequest request) {
        logger.info("Setting stock for product ID: {} to quantity: {}", productId, request.getQuantity());

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        int oldQuantity = inventory.getQuantity();
        inventory.setQuantity(request.getQuantity());

        Inventory saved = inventoryRepository.save(inventory);

        logger.info("Stock updated for product: {} (SKU: {}), old quantity: {}, new quantity: {}",
                saved.getProduct().getName(),
                saved.getProduct().getSku(),
                oldQuantity,
                saved.getQuantity());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(UUID productId, AdjustStockRequest request) {
        logger.info("Adjusting stock for product ID: {} by: {}", productId, request.getAdjustment());

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        int currentQuantity = inventory.getQuantity();
        int newQuantity = currentQuantity + request.getAdjustment();

        // Validate that adjustment doesn't result in negative stock
        if (newQuantity < 0) {
            logger.warn("Stock adjustment rejected for product: {} (SKU: {}), would result in negative stock. " +
                    "Current: {}, adjustment: {}, resulting: {}",
                    inventory.getProduct().getName(),
                    inventory.getProduct().getSku(),
                    currentQuantity,
                    request.getAdjustment(),
                    newQuantity);

            throw new InsufficientStockException(
                    inventory.getProduct().getName(),
                    currentQuantity,
                    Math.abs(request.getAdjustment()));
        }

        inventory.setQuantity(newQuantity);
        Inventory saved = inventoryRepository.save(inventory);

        logger.info("Stock adjusted for product: {} (SKU: {}), old quantity: {}, adjustment: {}, new quantity: {}",
                saved.getProduct().getName(),
                saved.getProduct().getSku(),
                currentQuantity,
                request.getAdjustment(),
                saved.getQuantity());

        return mapToResponse(saved);
    }

    @Override
    public void decrementStock(UUID productId, int quantity) {
        logger.debug("Decrementing stock for product ID: {} by quantity: {}", productId, quantity);

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        int currentQuantity = inventory.getQuantity();

        // Check if sufficient stock is available
        if (currentQuantity < quantity) {
            logger.warn("Insufficient stock for product: {} (SKU: {}), requested: {}, available: {}",
                    inventory.getProduct().getName(),
                    inventory.getProduct().getSku(),
                    quantity,
                    currentQuantity);

            throw new InsufficientStockException(
                    inventory.getProduct().getName(),
                    currentQuantity,
                    quantity);
        }

        inventory.setQuantity(currentQuantity - quantity);
        inventoryRepository.save(inventory);

        logger.info(
                "Stock decremented for product: {} (SKU: {}), old quantity: {}, decremented by: {}, new quantity: {}",
                inventory.getProduct().getName(),
                inventory.getProduct().getSku(),
                currentQuantity,
                quantity,
                inventory.getQuantity());
    }

    @Override
    public void restoreStock(UUID productId, int quantity) {
        logger.info("Restoring stock for product ID: {} by quantity: {}", productId, quantity);

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        int currentQuantity = inventory.getQuantity();
        inventory.setQuantity(currentQuantity + quantity);

        inventoryRepository.save(inventory);

        logger.info("Stock restored for product: {} (SKU: {}), old quantity: {}, restored by: {}, new quantity: {}",
                inventory.getProduct().getName(),
                inventory.getProduct().getSku(),
                currentQuantity,
                quantity,
                inventory.getQuantity());
    }

    /**
     * Finds inventory by product ID or throws ResourceNotFoundException.
     *
     * @param productId the product ID
     * @return the inventory entity
     * @throws ResourceNotFoundException if inventory not found
     */
    private Inventory findInventoryByProductIdOrThrow(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    logger.error("Inventory not found for product ID: {}", productId);
                    return new ResourceNotFoundException("Inventory", "productId", productId);
                });
    }

    /**
     * Maps Inventory entity to InventoryResponse DTO.
     *
     * @param inventory the inventory entity
     * @return the inventory response DTO
     */
    private InventoryResponse mapToResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        return new InventoryResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                inventory.getQuantity(),
                inventory.getUpdatedAt());
    }
}
