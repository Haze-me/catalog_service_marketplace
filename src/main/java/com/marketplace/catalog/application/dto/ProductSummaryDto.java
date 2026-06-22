package com.marketplace.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Compact product representation for list/search/browse views.
 * Equivalent to Django's ProductListSerializer.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDto {

    private UUID productId;
    private String name;
    private String brand;
    private BigDecimal price;
    private String slug;
    private String primaryImageUrl;
    private Boolean inStock;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private UUID categoryId;
}