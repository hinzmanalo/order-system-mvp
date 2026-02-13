package com.orderhub.catalog.service;

import com.orderhub.catalog.dto.ProductRequest;
import com.orderhub.catalog.dto.ProductResponse;
import com.orderhub.catalog.dto.ProductStatusRequest;
import com.orderhub.catalog.entity.Product;
import com.orderhub.catalog.repository.ProductRepository;
import com.orderhub.common.exception.DuplicateResourceException;
import com.orderhub.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementation of ProductService for managing product catalog operations.
 * <p>
 * Handles product CRUD operations, SKU uniqueness validation, and coordinates
 * with inventory management for product creation.
 * </p>
 * <p>
 * Thread-safety: Service methods are thread-safe when used within Spring's
 * transaction management context.
 * </p>
 *
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    // TODO: Inject InventoryRepository when inventory module is implemented (Phase
    // 08)
    // private final InventoryRepository inventoryRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Page<ProductResponse> getActiveProducts(String name, BigDecimal minPrice,
            BigDecimal maxPrice, Pageable pageable) {
        logger.debug("Fetching active products with filters - name: {}, minPrice: {}, maxPrice: {}",
                name, minPrice, maxPrice);

        Page<Product> products = productRepository.findActiveProducts(name, minPrice, maxPrice, pageable);

        logger.info("Found {} active products (page {} of {})",
                products.getNumberOfElements(), products.getNumber() + 1, products.getTotalPages());

        return products.map(this::mapToResponse);
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        logger.debug("Fetching product by ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product", "id", id);
                });

        logger.info("Retrieved product: {} (SKU: {})", product.getName(), product.getSku());

        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        logger.info("Creating new product with SKU: {}", request.getSku());

        // Check for duplicate SKU
        if (productRepository.existsBySku(request.getSku())) {
            logger.warn("Duplicate SKU detected: {}", request.getSku());
            throw new DuplicateResourceException("Product", "SKU", request.getSku());
        }

        // Create product entity
        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getSku());

        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully with ID: {} and SKU: {}",
                savedProduct.getId(), savedProduct.getSku());

        // TODO: Create inventory record when inventory module is implemented
        // Inventory inventory = new Inventory();
        // inventory.setProduct(savedProduct);
        // inventory.setQuantity(0);
        // inventoryRepository.save(inventory);
        // logger.info("Inventory record created for product ID: {}",
        // savedProduct.getId());

        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        logger.info("Updating product ID: {} with SKU: {}", id, request.getSku());

        // Find existing product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found for update with ID: {}", id);
                    return new ResourceNotFoundException("Product", "id", id);
                });

        // Check SKU uniqueness (excluding current product)
        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            logger.warn("Duplicate SKU detected during update: {}", request.getSku());
            throw new DuplicateResourceException("Product", "SKU", request.getSku());
        }

        // Update fields
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());

        Product updatedProduct = productRepository.save(product);
        logger.info("Product updated successfully: {} (SKU: {})",
                updatedProduct.getName(), updatedProduct.getSku());

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProductStatus(UUID id, ProductStatusRequest request) {
        logger.info("Updating product status for ID: {} to active={}", id, request.getActive());

        // Find existing product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Product not found for status update with ID: {}", id);
                    return new ResourceNotFoundException("Product", "id", id);
                });

        // Update active status
        product.setActive(request.getActive());

        Product updatedProduct = productRepository.save(product);
        logger.info("Product status updated: {} is now {}",
                updatedProduct.getName(), updatedProduct.isActive() ? "active" : "inactive");

        return mapToResponse(updatedProduct);
    }

    /**
     * Maps a Product entity to ProductResponse DTO.
     *
     * @param product the product entity
     * @return the product response DTO
     */
    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.isActive(),
                product.getCreatedAt());
    }
}
