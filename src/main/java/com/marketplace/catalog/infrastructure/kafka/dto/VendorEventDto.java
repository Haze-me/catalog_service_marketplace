package com.marketplace.catalog.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Incoming Kafka message shape for vendor.approved / vendor.suspended events.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorEventDto {

    private String eventId;
    private String eventType;
    private String timestamp;
    private VendorPayload payload;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VendorPayload {
        private UUID vendorId;
        private String businessName;
        private String businessEmail;
    }
}