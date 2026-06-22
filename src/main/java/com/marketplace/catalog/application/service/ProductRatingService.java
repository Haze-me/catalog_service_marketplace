package com.marketplace.catalog.application.service;

import com.marketplace.catalog.domain.model.CatalogProduct;
import com.marketplace.catalog.domain.model.ProductRating;
import com.marketplace.catalog.domain.repository.CatalogProductRepository;
import com.marketplace.catalog.domain.repository.ProductRatingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Maintains the product_ratings aggregate table and keeps
 * CatalogProduct.averageRating / reviewCount denormalised in sync.
 *
 * Note: this service does NOT store individual reviews — that data
 * lives entirely in the Commerce Service's reviews table. This service
 * only tracks the AGGREGATE (average + count), recalculated whenever
 * a review.created / review.updated / review.deleted event arrives.
 *
 * Since the Catalog Service has no access to Commerce Service's database,
 * recalculation here works off a running aggregate rather than re-scanning
 * raw review rows (which we don't have). The Commerce Service will need
 * to publish the rating delta or current aggregate in the event payload —
 * flagged as a coordination point for Phase 3.
 *
 * For now, this implementation recalculates using a simple incremental
 * approach based on what the event payload provides.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRatingService {

    private final ProductRatingRepository ratingRepository;
    private final CatalogProductRepository productRepository;

    /**
     * Recalculate and persist the rating aggregate for a product.
     *
     * IMPORTANT DESIGN NOTE: since this service doesn't own raw review data,
     * true recalculation (re-averaging all reviews) must happen in the
     * Commerce Service, which DOES own that data. The Commerce Service
     * should publish the freshly computed average_rating and review_count
     * directly in the review.created/updated/deleted event payload.
     *
     * This method, as currently scoped, is a placeholder that will be
     * completed in Phase 3 once the Commerce Service's review payload
     * shape is finalised. For now it logs the event and leaves existing
     * values unchanged, rather than silently corrupting good data with
     * a guess.
     */
    @Transactional
    public void recalculateRating(UUID productId) {
        log.warn(
                "recalculateRating called for productId={} — full implementation "
                        + "deferred until Phase 3 (Commerce Service) defines the review event "
                        + "payload shape with average_rating and review_count included.",
                productId
        );
        // Intentionally a no-op for now. Will be completed in Phase 3
        // when we control both sides of this contract.
    }

    /**
     * Apply a freshly computed rating aggregate (called once Phase 3 wires
     * the real review event payload with averageRating + reviewCount included).
     */
    @Transactional
    public void applyRatingAggregate(UUID productId, BigDecimal averageRating, Integer reviewCount) {
        BigDecimal rounded = averageRating.setScale(2, RoundingMode.HALF_UP);

        ProductRating rating = ratingRepository.findByProductId(productId)
                .orElse(ProductRating.builder().productId(productId).build());
        rating.setAverageRating(rounded);
        rating.setReviewCount(reviewCount);
        ratingRepository.save(rating);

        // Keep the denormalised fields on CatalogProduct in sync for fast list rendering
        productRepository.findById(productId).ifPresent(product -> {
            product.setAverageRating(rounded);
            product.setReviewCount(reviewCount);
            productRepository.save(product);
        });

        log.info("Applied rating aggregate | productId={} avgRating={} reviewCount={}",
                productId, rounded, reviewCount);
    }
}