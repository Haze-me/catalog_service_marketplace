package com.marketplace.catalog.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Incoming Kafka message shape for category.created / category.updated events.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryEventDto {

    private String eventId;
    private String eventType;
    private String timestamp;
    private CategoryPayload payload;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryPayload {
        private UUID categoryId;
        private String name;
        private String description;
        private String slug;
        private UUID parentId;
        private Boolean isActive;
    }
}