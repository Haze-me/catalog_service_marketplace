package com.marketplace.catalog.application.service;

import com.marketplace.catalog.application.dto.CategoryDto;
import com.marketplace.catalog.domain.model.CatalogCategory;
import com.marketplace.catalog.domain.repository.CatalogCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-side query service for categories — serves the public browsing API.
 * Separate from CatalogCategoryService (Module 3/4), which only handles
 * Kafka event writes. This class only reads.
 */
@Service
@RequiredArgsConstructor
public class CatalogCategoryQueryService {

    private final CatalogCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategoriesWithChildren() {
        List<CatalogCategory> topLevel = categoryRepository.findByParentIdIsNullAndIsActiveTrue();

        return topLevel.stream()
                .map(this::toDtoWithChildren)
                .toList();
    }

    private CategoryDto toDtoWithChildren(CatalogCategory category) {
        List<CategoryDto> children = categoryRepository
                .findByParentIdAndIsActiveTrue(category.getCategoryId())
                .stream()
                .map(this::toDto)
                .toList();

        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .slug(category.getSlug())
                .parentId(category.getParentId())
                .children(children)
                .build();
    }

    private CategoryDto toDto(CatalogCategory category) {
        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .slug(category.getSlug())
                .parentId(category.getParentId())
                .children(List.of())
                .build();
    }
}