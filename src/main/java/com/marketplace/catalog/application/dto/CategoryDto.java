package com.marketplace.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Category representation for the public catalog API.
 * Equivalent to Django's CategoryListSerializer / CategoryDetailSerializer.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private UUID categoryId;
    private String name;
    private String description;
    private String slug;
    private UUID parentId;
    private String parentName;
    private List<CategoryDto> children;
}