package com.marketplace.catalog.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.application.service.CatalogProductService;
import com.marketplace.catalog.infrastructure.kafka.dto.VendorEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VendorEventConsumer {

    private final CatalogProductService catalogProductService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "vendor.suspended", groupId = "${spring.kafka.consumer.group-id}")
    public void handleVendorSuspended(String rawMessage, Acknowledgment ack) {
        try {
            VendorEventDto event = objectMapper.readValue(rawMessage, VendorEventDto.class);
            log.info("Received vendor.suspended event | vendorId={}", event.getPayload().getVendorId());
            catalogProductService.hideAllProductsForVendor(event.getPayload().getVendorId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process vendor.suspended event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "vendor.approved", groupId = "${spring.kafka.consumer.group-id}")
    public void handleVendorApproved(String rawMessage, Acknowledgment ack) {
        try {
            VendorEventDto event = objectMapper.readValue(rawMessage, VendorEventDto.class);
            log.info("Received vendor.approved event | vendorId={}", event.getPayload().getVendorId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process vendor.approved event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}