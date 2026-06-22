package com.marketplace.catalog.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Incoming Kafka message shape for product.deleted events.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDeletedEventDto {

    private String eventId;
    private String eventType;
    private String timestamp;
    private DeletedPayload payload;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeletedPayload {
        private UUID productId;
    }
}