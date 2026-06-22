package com.marketplace.catalog.application.service;

import com.marketplace.catalog.domain.model.CatalogCategory;
import com.marketplace.catalog.domain.repository.CatalogCategoryRepository;
import com.marketplace.catalog.infrastructure.kafka.dto.CategoryEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the catalog_categories read model.
 * Populated via category.created / category.updated Kafka events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogCategoryService {

    private final CatalogCategoryRepository categoryRepository;

    @Transactional
    public void upsertCategory(CategoryEventDto.CategoryPayload payload) {
        CatalogCategory category = categoryRepository.findById(payload.getCategoryId())
                .orElse(new CatalogCategory());

        category.setCategoryId(payload.getCategoryId());
        category.setName(payload.getName());
        category.setDescription(payload.getDescription());
        category.setSlug(payload.getSlug());
        category.setParentId(payload.getParentId());
        category.setIsActive(payload.getIsActive() != null ? payload.getIsActive() : true);

        categoryRepository.save(category);
        log.info("Upserted catalog category | categoryId={} name={}",
                category.getCategoryId(), category.getName());
    }
}