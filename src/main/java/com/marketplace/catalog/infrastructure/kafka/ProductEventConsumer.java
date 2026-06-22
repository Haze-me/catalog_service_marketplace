package com.marketplace.catalog.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.application.service.CatalogProductService;
import com.marketplace.catalog.infrastructure.kafka.dto.ProductDeletedEventDto;
import com.marketplace.catalog.infrastructure.kafka.dto.ProductEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final CatalogProductService catalogProductService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "product.created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductCreated(String rawMessage, Acknowledgment ack) {
        try {
            ProductEventDto event = objectMapper.readValue(rawMessage, ProductEventDto.class);
            log.info("Received product.created event | productId={}", event.getPayload().getProductId());
            catalogProductService.upsertProduct(event.getPayload());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process product.created event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "product.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductUpdated(String rawMessage, Acknowledgment ack) {
        try {
            ProductEventDto event = objectMapper.readValue(rawMessage, ProductEventDto.class);
            log.info("Received product.updated event | productId={}", event.getPayload().getProductId());
            catalogProductService.upsertProduct(event.getPayload());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process product.updated event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "product.deleted", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductDeleted(String rawMessage, Acknowledgment ack) {
        try {
            ProductDeletedEventDto event = objectMapper.readValue(rawMessage, ProductDeletedEventDto.class);
            log.info("Received product.deleted event | productId={}", event.getPayload().getProductId());
            catalogProductService.deleteProduct(event.getPayload().getProductId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process product.deleted event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}