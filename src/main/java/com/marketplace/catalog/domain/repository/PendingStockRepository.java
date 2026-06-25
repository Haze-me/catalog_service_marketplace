package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.model.PendingStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for buffered stock statuses awaiting their product.
 */
public interface PendingStockRepository extends JpaRepository<PendingStock, UUID> {
}
