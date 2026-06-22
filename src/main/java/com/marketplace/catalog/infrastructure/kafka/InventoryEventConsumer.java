package com.marketplace.catalog.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.application.service.CatalogProductService;
import com.marketplace.catalog.infrastructure.kafka.dto.InventoryEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final CatalogProductService catalogProductService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "inventory.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryUpdated(String rawMessage, Acknowledgment ack) {
        try {
            InventoryEventDto event = objectMapper.readValue(rawMessage, InventoryEventDto.class);
            log.info("Received inventory.updated event | productId={} inStock={}",
                    event.getPayload().getProductId(), event.getPayload().getInStock());
            catalogProductService.updateStockStatus(
                    event.getPayload().getProductId(),
                    event.getPayload().getInStock()
            );
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process inventory.updated event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}