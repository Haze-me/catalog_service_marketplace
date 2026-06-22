package com.marketplace.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Full product representation for the product detail page.
 * Equivalent to Django's ProductDetailSerializer.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDto {

    private UUID productId;
    private UUID vendorId;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String brand;
    private String sku;
    private BigDecimal price;
    private String slug;
    private String status;
    private String primaryImageUrl;
    private Boolean inStock;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}