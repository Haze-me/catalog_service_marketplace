package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.model.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProductRating aggregate.
 */
public interface ProductRatingRepository extends JpaRepository<ProductRating, UUID> {

    Optional<ProductRating> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);
}