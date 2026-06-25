package com.marketplace.catalog.api.controller;

import com.marketplace.catalog.application.dto.ApiResponseDto;
import com.marketplace.catalog.application.dto.PagedResponseDto;
import com.marketplace.catalog.application.dto.ProductDetailDto;
import com.marketplace.catalog.application.dto.ProductSummaryDto;
import com.marketplace.catalog.application.service.CatalogProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public-facing product discovery API.
 * Read-only — all data originates from Kafka events, never written here.
 */
@Tag(name = "Catalog Products", description = "Product discovery, search, and filtering")
@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class CatalogProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CatalogProductService catalogProductService;

    @Operation(summary = "Browse active products with optional filters, paginated")
    @GetMapping
    public ApiResponseDto<PagedResponseDto<ProductSummaryDto>> browseProducts(
            @Parameter(description = "Page number, 1-indexed") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort: price_asc, price_desc, newest, rating — or a field name")
            @RequestParam(defaultValue = "newest") String sortBy,
            @Parameter(description = "Sort direction: asc or desc (used with a field name)")
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Keyword search over name and brand")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            HttpServletRequest request
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        PagedResponseDto<ProductSummaryDto> result = catalogProductService.browseProducts(
                search, categoryId, brand, minPrice, maxPrice, pageable);
        return ApiResponseDto.success(result, "Success", request.getRequestURI());
    }

    @Operation(summary = "Get full product detail by slug")
    @GetMapping("/{slug}")
    @Cacheable(value = "products", key = "#slug")
    public ApiResponseDto<ProductDetailDto> getProductBySlug(
            @PathVariable String slug,
            HttpServletRequest request
    ) {
        ProductDetailDto product = catalogProductService.getProductBySlug(slug);
        return ApiResponseDto.success(product, "Success", request.getRequestURI());
    }

    @Operation(summary = "List products in a specific category")
    @GetMapping("/category/{categoryId}")
    public ApiResponseDto<PagedResponseDto<ProductSummaryDto>> getProductsByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
        PagedResponseDto<ProductSummaryDto> result = catalogProductService.getProductsByCategory(categoryId, pageable);
        return ApiResponseDto.success(result, "Success", request.getRequestURI());
    }

    @Operation(summary = "List products from a specific vendor")
    @GetMapping("/vendor/{vendorId}")
    public ApiResponseDto<PagedResponseDto<ProductSummaryDto>> getProductsByVendor(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
        PagedResponseDto<ProductSummaryDto> result = catalogProductService.getProductsByVendor(vendorId, pageable);
        return ApiResponseDto.success(result, "Success", request.getRequestURI());
    }

    @Operation(summary = "Filter products by price range")
    @GetMapping("/filter/price")
    public ApiResponseDto<PagedResponseDto<ProductSummaryDto>> filterByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice.");
        }
        if (minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minPrice cannot be negative.");
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
        PagedResponseDto<ProductSummaryDto> result =
                catalogProductService.filterByPriceRange(minPrice, maxPrice, pageable);
        return ApiResponseDto.success(result, "Success", request.getRequestURI());
    }

    @Operation(summary = "Filter products by brand")
    @GetMapping("/filter/brand")
    public ApiResponseDto<PagedResponseDto<ProductSummaryDto>> filterByBrand(
            @RequestParam String brand,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
        PagedResponseDto<ProductSummaryDto> result = catalogProductService.filterByBrand(brand, pageable);
        return ApiResponseDto.success(result, "Success", request.getRequestURI());
    }

    @Operation(summary = "Get product detail by product ID (for internal service-to-service calls)")
    @GetMapping("/by-id/{productId}")
    public ApiResponseDto<ProductDetailDto> getProductById(
            @PathVariable UUID productId,
            HttpServletRequest request
    ) {
        ProductDetailDto product = catalogProductService.getProductById(productId);
        return ApiResponseDto.success(product, "Success", request.getRequestURI());
    }

    @Operation(summary = "Full-text search products by keyword")
    @GetMapping("/search")
    public ApiResponseDto<PagedResponseDto<ProductSummaryDto>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("Search query 'q' must not be empty.");
        }
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), clampSize(size));
        PagedResponseDto<ProductSummaryDto> result = catalogProductService.searchProducts(q.trim(), pageable);
        return ApiResponseDto.success(result, "Success", request.getRequestURI());
    }

    /** Sortable entity fields the client is allowed to sort by. */
    private static final java.util.Set<String> SORTABLE_FIELDS =
            java.util.Set.of("name", "price", "averageRating", "createdAt");

    /**
     * Builds a Pageable with 1-indexed page numbers (client-facing) converted
     * to Spring's 0-indexed internal pages, plus safe sorting.
     */
    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        return PageRequest.of(Math.max(page - 1, 0), clampSize(size), resolveSort(sortBy, direction));
    }

    /**
     * Resolves a sort instruction from either a combined token
     * (price_asc, price_desc, newest, rating) or a field name + direction.
     * Falls back to newest-first for anything unrecognised, so a bad value
     * never throws a 500.
     */
    private Sort resolveSort(String sortBy, String direction) {
        if (sortBy != null) {
            switch (sortBy) {
                case "price_asc":  return Sort.by(Sort.Direction.ASC, "price");
                case "price_desc": return Sort.by(Sort.Direction.DESC, "price");
                case "newest":     return Sort.by(Sort.Direction.DESC, "createdAt");
                case "rating":     return Sort.by(Sort.Direction.DESC, "averageRating");
                default: break;
            }
        }
        if (sortBy != null && SORTABLE_FIELDS.contains(sortBy)) {
            Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
            return Sort.by(dir, sortBy);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    /**
     * Caps page size to MAX_PAGE_SIZE to prevent clients from requesting
     * unreasonably large result sets that would strain the database.
     */
    private int clampSize(int size) {
        if (size < 1) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }
}