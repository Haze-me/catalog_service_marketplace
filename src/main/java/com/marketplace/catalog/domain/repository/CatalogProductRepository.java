package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.model.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CatalogProduct read-model.
 * Spring Data JPA auto-generates the implementation at runtime —
 * we only declare method signatures here.
 *
 * Equivalent to Django: Product.objects.filter(...) / .get(...)
 */
public interface CatalogProductRepository
        extends JpaRepository<CatalogProduct, UUID>,
                JpaSpecificationExecutor<CatalogProduct> {

    /**
     * Find a single product by its slug (used for product detail pages).
     * Equivalent to: Product.objects.get(slug=slug)
     */
    Optional<CatalogProduct> findBySlug(String slug);

    /**
     * Find a product by SKU — useful for de-duplication checks during Kafka consumption.
     */
    Optional<CatalogProduct> findBySku(String sku);

    /**
     * List all ACTIVE products belonging to a specific vendor, paginated.
     */
    Page<CatalogProduct> findByVendorIdAndStatus(UUID vendorId, String status, Pageable pageable);

    /**
     * List all ACTIVE products in a specific category, paginated.
     */
    Page<CatalogProduct> findByCategoryIdAndStatus(UUID categoryId, String status, Pageable pageable);

    /**
     * List all products with a given status (e.g. all ACTIVE products), paginated.
     * Used for the general "browse all products" endpoint.
     */
    Page<CatalogProduct> findByStatus(String status, Pageable pageable);

    /**
     * Filter products by price range and status.
     */
    Page<CatalogProduct> findByStatusAndPriceBetween(
            String status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    /**
     * Filter products by brand and status.
     */
    Page<CatalogProduct> findByStatusAndBrandIgnoreCase(
            String status,
            String brand,
            Pageable pageable
    );

    /**
     * Full-text search using PostgreSQL's tsvector search_vector column.
     * The @@ operator matches the search_vector against a parsed search query.
     * plainto_tsquery safely converts user input into a tsquery, handling
     * special characters and multiple words automatically.
     *
     * Equivalent to Django: SearchVector + SearchQuery (django.contrib.postgres.search)
     */
    @Query(value = """
            SELECT * FROM catalog_products
            WHERE status = :status
            AND search_vector @@ plainto_tsquery('english', :searchTerm)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :searchTerm)) DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM catalog_products
            WHERE status = :status
            AND search_vector @@ plainto_tsquery('english', :searchTerm)
            """,
            nativeQuery = true)
    Page<CatalogProduct> searchByKeyword(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Fuzzy name search using pg_trgm similarity — useful for typo-tolerant search
     * as a fallback when full-text search returns no results.
     */
    @Query(value = """
            SELECT * FROM catalog_products
            WHERE status = :status
            AND name % :searchTerm
            ORDER BY similarity(name, :searchTerm) DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM catalog_products
            WHERE status = :status
            AND name % :searchTerm
            """,
            nativeQuery = true)
    Page<CatalogProduct> fuzzySearchByName(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * Check whether a product already exists (used to decide INSERT vs UPDATE
     * when consuming Kafka events).
     */
    boolean existsByProductId(UUID productId);
}