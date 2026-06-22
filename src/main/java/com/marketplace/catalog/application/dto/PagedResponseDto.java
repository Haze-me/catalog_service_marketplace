package com.marketplace.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Matches the exact shape used by the Django Admin & Vendor Service's
 * StandardResultsPagination, so frontend clients get a consistent
 * pagination contract across both services.
 *
 * {
 *   "content": [...],
 *   "page": 1,
 *   "size": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "hasNext": true,
 *   "hasPrevious": false
 * }
 */
@Getter
@Builder
@AllArgsConstructor
public class PagedResponseDto<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PagedResponseDto<T> from(Page<T> springPage) {
        return PagedResponseDto.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber() + 1)   // Spring is 0-indexed; we expose 1-indexed
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .hasNext(springPage.hasNext())
                .hasPrevious(springPage.hasPrevious())
                .build();
    }
}