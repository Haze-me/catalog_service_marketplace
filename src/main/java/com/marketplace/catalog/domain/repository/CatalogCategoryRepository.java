package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.model.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CatalogCategory read-model.
 */
public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {

    Optional<CatalogCategory> findBySlug(String slug);

    List<CatalogCategory> findByIsActiveTrue();

    List<CatalogCategory> findByParentIdAndIsActiveTrue(UUID parentId);

    List<CatalogCategory> findByParentIdIsNullAndIsActiveTrue();

    boolean existsByCategoryId(UUID categoryId);
}