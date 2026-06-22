package com.marketplace.catalog.api.controller;

import com.marketplace.catalog.application.dto.ApiResponseDto;
import com.marketplace.catalog.application.dto.CategoryDto;
import com.marketplace.catalog.application.service.CatalogCategoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public-facing category browsing API.
 */
@Tag(name = "Catalog Categories", description = "Category browsing")
@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CatalogCategoryController {

    private final CatalogCategoryQueryService categoryQueryService;

    @Operation(summary = "List all active top-level categories with their children nested")
    @GetMapping
    @Cacheable(value = "categories", key = "'all'")
    public ApiResponseDto<List<CategoryDto>> listCategories(HttpServletRequest request) {
        List<CategoryDto> categories = categoryQueryService.getActiveCategoriesWithChildren();
        return ApiResponseDto.success(categories, "Success", request.getRequestURI());
    }
}