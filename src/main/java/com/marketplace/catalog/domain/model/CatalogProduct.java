package com.marketplace.catalog.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-model representation of a product in the catalog.
 * This table is populated ENTIRELY via Kafka events from the
 * Admin & Vendor Service — never written to via direct API calls.
 *
 * CQRS pattern: this is the "Query" side. The "Command" side
 * (actual product creation/editing) lives in the Django Admin & Vendor Service.
 */
@Entity
@Table(name = "catalog_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogProduct {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "primary_image_url")
    private String primaryImageUrl;

    @Column(name = "in_stock", nullable = false)
    private Boolean inStock;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.averageRating == null) {
            this.averageRating = BigDecimal.ZERO;
        }
        if (this.reviewCount == null) {
            this.reviewCount = 0;
        }
        if (this.inStock == null) {
            this.inStock = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}