package com.marketplace.catalog.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Incoming Kafka message shape for review.created / review.updated / review.deleted events,
 * published by the Commerce Service (Phase 3).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewEventDto {

    private String eventId;
    private String eventType;
    private String timestamp;
    private ReviewPayload payload;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReviewPayload {
        private UUID reviewId;
        private UUID productId;
        private UUID customerId;
        private Integer rating;
        private String comment;
    }
}