package com.marketplace.catalog.application.service;

import com.marketplace.catalog.application.dto.PagedResponseDto;
import com.marketplace.catalog.application.dto.ProductDetailDto;
import com.marketplace.catalog.application.dto.ProductSummaryDto;
import com.marketplace.catalog.domain.model.CatalogCategory;
import com.marketplace.catalog.domain.model.CatalogProduct;
import com.marketplace.catalog.domain.repository.CatalogCategoryRepository;
import com.marketplace.catalog.domain.repository.CatalogProductRepository;
import com.marketplace.catalog.infrastructure.kafka.dto.ProductEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Core service for the catalog product read model.
 *
 * Responsibilities:
 * - Upsert products from product.created / product.updated Kafka events.
 * - Soft-remove products from product.deleted events.
 * - Update stock status from inventory.updated events.
 * - Hide all of a vendor's products on vendor.suspended events.
 * - Serve search, filter, and detail queries to the public-facing API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogProductService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final CatalogProductRepository productRepository;
    private final CatalogCategoryRepository categoryRepository;

    // -----------------------------------------------------------------
    // Kafka event handlers (write side of CQRS)
    // -----------------------------------------------------------------

    /**
     * Insert or update a catalog product from a product.created / product.updated event.
     * Idempotent: safe to call multiple times with the same payload (Kafka at-least-once
     * delivery means this WILL be called more than once occasionally).
     */
    @Transactional
    public void upsertProduct(ProductEventDto.ProductPayload payload) {
        CatalogProduct product = productRepository.findById(payload.getProductId())
                .orElse(new CatalogProduct());

        product.setProductId(payload.getProductId());
        product.setVendorId(payload.getVendorId());
        product.setCategoryId(payload.getCategoryId());
        product.setName(payload.getName());
        product.setDescription(payload.getDescription());
        product.setBrand(payload.getBrand());
        product.setSku(payload.getSku());
        product.setPrice(payload.getPrice());
        product.setSlug(payload.getSlug());
        product.setStatus(payload.getStatus());
        product.setPrimaryImageUrl(payload.getPrimaryImageUrl());

        // Preserve existing inStock / rating values if this is an update,
        // since product.updated events don't carry stock or rating data.
        if (product.getInStock() == null) {
            product.setInStock(false);
        }
        if (product.getAverageRating() == null) {
            product.setAverageRating(BigDecimal.ZERO);
        }
        if (product.getReviewCount() == null) {
            product.setReviewCount(0);
        }

        productRepository.save(product);
        log.info("Upserted catalog product | productId={} name={}", product.getProductId(), product.getName());
    }

    /**
     * Remove a product from the catalog read model on product.deleted event.
     * Hard delete here is acceptable — this is a read model, not the
     * source of truth. The Django service retains the soft-deleted record.
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        if (productRepository.existsById(productId)) {
            productRepository.deleteById(productId);
            log.info("Deleted catalog product | productId={}", productId);
        } else {
            log.warn("Attempted to delete non-existent catalog product | productId={}", productId);
        }
    }

    /**
     * Update the denormalised in_stock flag from an inventory.updated event.
     */
    @Transactional
    public void updateStockStatus(UUID productId, Boolean inStock) {
        productRepository.findById(productId).ifPresentOrElse(
                product -> {
                    product.setInStock(inStock);
                    productRepository.save(product);
                    log.info("Updated stock status | productId={} inStock={}", productId, inStock);
                },
                () -> log.warn("Received inventory update for unknown product | productId={}", productId)
        );
    }

    /**
     * Hide all products belonging to a suspended vendor.
     * We mark them SUSPENDED rather than deleting — this can be reversed
     * if the vendor is later re-approved and republishes their products.
     */
    @Transactional
    public void hideAllProductsForVendor(UUID vendorId) {
        Pageable allOfThem = Pageable.unpaged();
        Page<CatalogProduct> vendorProducts = productRepository
                .findByVendorIdAndStatus(vendorId, STATUS_ACTIVE, allOfThem);

        List<CatalogProduct> products = vendorProducts.getContent();
        products.forEach(p -> p.setStatus(STATUS_SUSPENDED));
        productRepository.saveAll(products);

        log.info("Hid {} products for suspended vendor | vendorId={}", products.size(), vendorId);
    }

    // -----------------------------------------------------------------
    // Public query API (read side of CQRS)
    // -----------------------------------------------------------------

    /**
     * Browse all active products, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductSummaryDto> browseProducts(Pageable pageable) {
        Page<CatalogProduct> page = productRepository.findByStatus(STATUS_ACTIVE, pageable);
        return PagedResponseDto.from(page.map(this::toSummaryDto));
    }

    /**
     * Get full detail for a single product by slug.
     * Throws NoSuchElementException if not found — handled by the global
     * exception handler in Module 6.
     */
    @Transactional(readOnly = true)
    public ProductDetailDto getProductBySlug(String slug) {
        CatalogProduct product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + slug));
        return toDetailDto(product);
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProductById(UUID productId) {
        CatalogProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        return toDetailDto(product);
    }

    /**
     * List products within a specific category, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductSummaryDto> getProductsByCategory(UUID categoryId, Pageable pageable) {
        Page<CatalogProduct> page = productRepository.findByCategoryIdAndStatus(categoryId, STATUS_ACTIVE, pageable);
        return PagedResponseDto.from(page.map(this::toSummaryDto));
    }

    /**
     * List products from a specific vendor, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductSummaryDto> getProductsByVendor(UUID vendorId, Pageable pageable) {
        Page<CatalogProduct> page = productRepository.findByVendorIdAndStatus(vendorId, STATUS_ACTIVE, pageable);
        return PagedResponseDto.from(page.map(this::toSummaryDto));
    }

    /**
     * Filter products by price range.
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductSummaryDto> filterByPriceRange(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<CatalogProduct> page = productRepository
                .findByStatusAndPriceBetween(STATUS_ACTIVE, minPrice, maxPrice, pageable);
        return PagedResponseDto.from(page.map(this::toSummaryDto));
    }

    /**
     * Filter products by brand.
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductSummaryDto> filterByBrand(String brand, Pageable pageable) {
        Page<CatalogProduct> page = productRepository
                .findByStatusAndBrandIgnoreCase(STATUS_ACTIVE, brand, pageable);
        return PagedResponseDto.from(page.map(this::toSummaryDto));
    }

    /**
     * Full-text search. Falls back to fuzzy trigram search if no results found —
     * handles typos and partial matches gracefully.
     */
    @Transactional(readOnly = true)
    public PagedResponseDto<ProductSummaryDto> searchProducts(String query, Pageable pageable) {
        Page<CatalogProduct> page = productRepository.searchByKeyword(query, STATUS_ACTIVE, pageable);

        if (page.isEmpty()) {
            log.info("No full-text results for '{}', falling back to fuzzy search", query);
            page = productRepository.fuzzySearchByName(query, STATUS_ACTIVE, pageable);
        }

        return PagedResponseDto.from(page.map(this::toSummaryDto));
    }

    // -----------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------

    private ProductSummaryDto toSummaryDto(CatalogProduct product) {
        return ProductSummaryDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .brand(product.getBrand())
                .price(product.getPrice())
                .slug(product.getSlug())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .inStock(product.getInStock())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .categoryId(product.getCategoryId())
                .build();
    }

    private ProductDetailDto toDetailDto(CatalogProduct product) {
        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(CatalogCategory::getName)
                .orElse(null);

        return ProductDetailDto.builder()
                .productId(product.getProductId())
                .vendorId(product.getVendorId())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .sku(product.getSku())
                .price(product.getPrice())
                .slug(product.getSlug())
                .status(product.getStatus())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .inStock(product.getInStock())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}