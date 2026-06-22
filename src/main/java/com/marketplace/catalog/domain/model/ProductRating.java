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
 * Aggregated rating data for a product.
 * Updated via review.created / review.updated / review.deleted Kafka events
 * from the Commerce Service.
 *
 * This is denormalised onto CatalogProduct.averageRating / reviewCount too,
 * for fast list-view rendering without a join. This table is the
 * source of truth; CatalogProduct fields are a cache of it.
 */
@Entity
@Table(name = "product_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRating {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = OffsetDateTime.now();
        if (this.averageRating == null) {
            this.averageRating = BigDecimal.ZERO;
        }
        if (this.reviewCount == null) {
            this.reviewCount = 0;
        }
    }
}