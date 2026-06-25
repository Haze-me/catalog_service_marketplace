package com.marketplace.catalog.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Buffers an inventory.updated stock status that arrived before its product
 * existed in the catalog read model. Applied (and removed) when the matching
 * product.created event is processed.
 */
@Entity
@Table(name = "catalog_pending_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingStock {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "in_stock", nullable = false)
    private Boolean inStock;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
