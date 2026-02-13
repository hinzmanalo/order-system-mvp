package com.orderhub.inventory.service;

import com.orderhub.catalog.entity.Product;
import com.orderhub.common.exception.InsufficientStockException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.inventory.dto.AdjustStockRequest;
import com.orderhub.inventory.dto.InventoryResponse;
import com.orderhub.inventory.dto.SetStockRequest;
import com.orderhub.inventory.entity.Inventory;
import com.orderhub.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product createTestProduct() {
        Product product = new Product(
                "Test Product",
                "Description",
                new BigDecimal("99.99"),
                "TEST-SKU");
        product.setId(UUID.randomUUID());
        return product;
    }

    private Inventory createTestInventory(Product product, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(quantity);
        return inventory;
    }

    @Test
    @DisplayName("SetStock - success - quantity set to absolute value")
    void setStock_success() {
        // Given
        UUID productId = UUID.randomUUID();
        SetStockRequest request = new SetStockRequest();
        request.setQuantity(100);

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 50);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // When
        InventoryResponse response = inventoryService.setStock(productId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(100);

        verify(inventoryRepository).save(inventory);
        assertThat(inventory.getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("AdjustStock - positive adjustment - quantity increases")
    void adjustStock_positiveAdjustment() {
        // Given
        UUID productId = UUID.randomUUID();
        AdjustStockRequest request = new AdjustStockRequest();
        request.setAdjustment(50);

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // When
        InventoryResponse response = inventoryService.adjustStock(productId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(150);

        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("AdjustStock - negative adjustment - quantity decreases")
    void adjustStock_negativeAdjustment() {
        // Given
        UUID productId = UUID.randomUUID();
        AdjustStockRequest request = new AdjustStockRequest();
        request.setAdjustment(-30);

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // When
        InventoryResponse response = inventoryService.adjustStock(productId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("AdjustStock - below zero - throws InsufficientStockException")
    void adjustStock_belowZero_throws() {
        // Given
        UUID productId = UUID.randomUUID();
        AdjustStockRequest request = new AdjustStockRequest();
        request.setAdjustment(-150);

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        // When & Then
        assertThatThrownBy(() -> inventoryService.adjustStock(productId, request))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("DecrementStock - sufficient stock - quantity decremented")
    void decrementStock_sufficientStock() {
        // Given
        UUID productId = UUID.randomUUID();
        int decrementQty = 30;

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // When
        inventoryService.decrementStock(productId, decrementQty);

        // Then
        assertThat(inventory.getQuantity()).isEqualTo(70);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("DecrementStock - insufficient stock - throws InsufficientStockException")
    void decrementStock_insufficientStock_throws() {
        // Given
        UUID productId = UUID.randomUUID();
        int decrementQty = 150;

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        // When & Then
        assertThatThrownBy(() -> inventoryService.decrementStock(productId, decrementQty))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("RestoreStock - success - quantity increased")
    void restoreStock_success() {
        // Given
        UUID productId = UUID.randomUUID();
        int restoreQty = 50;

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 100);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        // When
        inventoryService.restoreStock(productId, restoreQty);

        // Then
        assertThat(inventory.getQuantity()).isEqualTo(150);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("OptimisticLock - conflict - verify exception handling")
    void optimisticLock_conflict() {
        // Given
        UUID productId = UUID.randomUUID();
        SetStockRequest request = new SetStockRequest();
        request.setQuantity(100);

        Product product = createTestProduct();
        Inventory inventory = createTestInventory(product, 50);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
                        "Inventory", inventory));

        // When & Then
        assertThatThrownBy(() -> inventoryService.setStock(productId, request))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }
}
