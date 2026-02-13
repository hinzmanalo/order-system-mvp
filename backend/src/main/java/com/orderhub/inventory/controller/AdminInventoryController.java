package com.orderhub.inventory.controller;

import com.orderhub.inventory.dto.AdjustStockRequest;
import com.orderhub.inventory.dto.InventoryResponse;
import com.orderhub.inventory.dto.SetStockRequest;
import com.orderhub.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for admin inventory management operations.
 * <p>
 * All endpoints require ADMIN role. Provides operations to view, set, and
 * adjust
 * inventory stock levels.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@Tag(name = "Admin Inventory", description = "Admin endpoints for inventory management")
@SecurityRequirement(name = "bearer-jwt")
public class AdminInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(AdminInventoryController.class);

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Retrieves paginated list of all inventory records.
     *
     * @param page the page number (default: 0)
     * @param size the page size (default: 20)
     * @return page of inventory responses
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all inventory", description = "Retrieves paginated list of all inventory with product details. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory list retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    public ResponseEntity<Page<InventoryResponse>> getAllInventory(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        logger.info("GET /api/v1/admin/inventory - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryResponse> inventory = inventoryService.getAllInventory(pageable);

        logger.debug("Returning {} inventory records", inventory.getNumberOfElements());

        return ResponseEntity.ok(inventory);
    }

    /**
     * Retrieves inventory for a specific product.
     *
     * @param productId the product ID
     * @return inventory response
     */
    @GetMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get inventory by product ID", description = "Retrieves inventory information for a specific product. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public ResponseEntity<InventoryResponse> getInventoryByProductId(
            @Parameter(description = "Product ID") @PathVariable UUID productId) {

        logger.info("GET /api/v1/admin/inventory/{} - fetching inventory", productId);

        InventoryResponse inventory = inventoryService.getInventoryByProductId(productId);

        return ResponseEntity.ok(inventory);
    }

    /**
     * Sets absolute stock quantity for a product.
     *
     * @param productId the product ID
     * @param request   the set stock request
     * @return updated inventory response
     */
    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set stock quantity", description = "Sets absolute stock quantity for a product. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - quantity must be non-negative"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Inventory not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic locking conflict - retry")
    })
    public ResponseEntity<InventoryResponse> setStock(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody SetStockRequest request) {

        logger.info("PUT /api/v1/admin/inventory/{} - setting stock to: {}", productId, request.getQuantity());

        InventoryResponse inventory = inventoryService.setStock(productId, request);

        logger.info("Stock set successfully for product ID: {}, new quantity: {}", productId, inventory.getQuantity());

        return ResponseEntity.ok(inventory);
    }

    /**
     * Adjusts stock by a relative amount.
     *
     * @param productId the product ID
     * @param request   the adjustment request
     * @return updated inventory response
     */
    @PatchMapping("/{productId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust stock quantity", description = "Adjusts stock by a relative amount (positive or negative). Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
            @ApiResponse(responseCode = "404", description = "Inventory not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock or optimistic locking conflict")
    })
    public ResponseEntity<InventoryResponse> adjustStock(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request) {

        logger.info("PATCH /api/v1/admin/inventory/{}/adjust - adjusting stock by: {}", productId,
                request.getAdjustment());

        InventoryResponse inventory = inventoryService.adjustStock(productId, request);

        logger.info("Stock adjusted successfully for product ID: {}, new quantity: {}", productId,
                inventory.getQuantity());

        return ResponseEntity.ok(inventory);
    }
}
