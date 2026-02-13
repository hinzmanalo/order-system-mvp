package com.orderhub.catalog.service;

import com.orderhub.catalog.dto.ProductRequest;
import com.orderhub.catalog.dto.ProductResponse;
import com.orderhub.catalog.dto.ProductStatusRequest;
import com.orderhub.catalog.entity.Product;
import com.orderhub.catalog.repository.ProductRepository;
import com.orderhub.common.exception.DuplicateResourceException;
import com.orderhub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("CreateProduct - success - creates product with inventory")
    void createProduct_success_createsInventory() {
        // Given
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("99.99"));
        request.setSku("TEST-SKU-001");

        when(productRepository.existsBySku(request.getSku())).thenReturn(false);

        Product savedProduct = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getSku());
        savedProduct.setId(UUID.randomUUID());

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        ProductResponse response = productService.createProduct(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getDescription()).isEqualTo(request.getDescription());
        assertThat(response.getPrice()).isEqualByComparingTo(request.getPrice());
        assertThat(response.getSku()).isEqualTo(request.getSku());
        assertThat(response.isActive()).isTrue();

        verify(productRepository).existsBySku(request.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("CreateProduct - duplicate SKU - throws DuplicateResourceException")
    void createProduct_duplicateSku_throws() {
        // Given
        ProductRequest request = new ProductRequest();
        request.setSku("DUPLICATE-SKU");

        when(productRepository.existsBySku(request.getSku())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UpdateProduct - success - fields updated")
    void updateProduct_success() {
        // Given
        UUID productId = UUID.randomUUID();
        ProductRequest request = new ProductRequest();
        request.setName("Updated Product");
        request.setDescription("Updated Description");
        request.setPrice(new BigDecimal("149.99"));
        request.setSku("UPDATED-SKU");

        Product existingProduct = new Product(
                "Old Product",
                "Old Description",
                new BigDecimal("99.99"),
                "OLD-SKU");
        existingProduct.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySkuAndIdNot(request.getSku(), productId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ProductResponse response = productService.updateProduct(productId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getDescription()).isEqualTo(request.getDescription());
        assertThat(response.getPrice()).isEqualByComparingTo(request.getPrice());
        assertThat(response.getSku()).isEqualTo(request.getSku());

        verify(productRepository).save(existingProduct);
    }

    @Test
    @DisplayName("UpdateProduct - SKU conflict - throws DuplicateResourceException")
    void updateProduct_skuConflict_throws() {
        // Given
        UUID productId = UUID.randomUUID();
        ProductRequest request = new ProductRequest();
        request.setSku("TAKEN-SKU");

        Product existingProduct = new Product(
                "Product",
                "Description",
                new BigDecimal("99.99"),
                "OLD-SKU");

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySkuAndIdNot(request.getSku(), productId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UpdateProduct - not found - throws ResourceNotFoundException")
    void updateProduct_notFound_throws() {
        // Given
        UUID productId = UUID.randomUUID();
        ProductRequest request = new ProductRequest();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UpdateProductStatus - deactivate - active set to false")
    void updateProductStatus_deactivate() {
        // Given
        UUID productId = UUID.randomUUID();
        ProductStatusRequest request = new ProductStatusRequest();
        request.setActive(false);

        Product product = new Product(
                "Active Product",
                "Description",
                new BigDecimal("99.99"),
                "SKU-001");
        product.setId(productId);
        product.setActive(true);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ProductResponse response = productService.updateProductStatus(productId, request);

        // Then
        assertThat(response.isActive()).isFalse();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("GetActiveProducts - filters inactive - only active products returned")
    void getActiveProducts_filtersInactive() {
        // Given
        Product activeProduct = new Product(
                "Active Product",
                "Description",
                new BigDecimal("99.99"),
                "SKU-001");
        activeProduct.setActive(true);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(activeProduct));

        when(productRepository.findActiveProducts(null, null, null, pageable))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getActiveProducts(null, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("GetActiveProducts - filter by name - name filter works")
    void getActiveProducts_filterByName() {
        // Given
        String nameFilter = "Test";
        Product product = new Product(
                "Test Product",
                "Description",
                new BigDecimal("99.99"),
                "SKU-001");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));

        when(productRepository.findActiveProducts(nameFilter, null, null, pageable))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getActiveProducts(nameFilter, null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Test");
    }

    @Test
    @DisplayName("GetActiveProducts - filter by price range - price filter works")
    void getActiveProducts_filterByPriceRange() {
        // Given
        BigDecimal minPrice = new BigDecimal("50.00");
        BigDecimal maxPrice = new BigDecimal("150.00");

        Product product = new Product(
                "Product",
                "Description",
                new BigDecimal("99.99"),
                "SKU-001");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));

        when(productRepository.findActiveProducts(null, minPrice, maxPrice, pageable))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getActiveProducts(null, minPrice, maxPrice, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        BigDecimal price = result.getContent().get(0).getPrice();
        assertThat(price).isGreaterThanOrEqualTo(minPrice);
        assertThat(price).isLessThanOrEqualTo(maxPrice);
    }

    @Test
    @DisplayName("GetProductById - not found - throws ResourceNotFoundException")
    void getProductById_notFound_throws() {
        // Given
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }
}
