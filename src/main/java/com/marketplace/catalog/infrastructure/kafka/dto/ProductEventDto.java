package com.marketplace.catalog.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Incoming Kafka message shape for product.created / product.updated events,
 * published by the Django Admin & Vendor Service.
 *
 * Matches events/producers.py's _build_event() envelope:
 * { "eventId": ..., "eventType": ..., "timestamp": ..., "payload": {...} }
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEventDto {

    private String eventId;
    private String eventType;
    private String timestamp;
    private ProductPayload payload;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductPayload {
        private UUID productId;
        private UUID vendorId;
        private UUID categoryId;
        private String name;
        private String description;
        private String brand;
        private String sku;
        private BigDecimal price;
        private String status;
        private String slug;
        private String primaryImageUrl;
    }
}