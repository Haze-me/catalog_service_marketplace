package com.marketplace.catalog.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.application.service.CatalogCategoryService;
import com.marketplace.catalog.infrastructure.kafka.dto.CategoryEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes category.created / category.updated events.
 * Receives the raw JSON string and parses it manually with ObjectMapper —
 * no automatic Spring type-mapping involved.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryEventConsumer {

    private final CatalogCategoryService catalogCategoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "category.created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCategoryCreated(String rawMessage, Acknowledgment ack) {
        processMessage(rawMessage, ack);
    }

    @KafkaListener(topics = "category.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCategoryUpdated(String rawMessage, Acknowledgment ack) {
        processMessage(rawMessage, ack);
    }

    private void processMessage(String rawMessage, Acknowledgment ack) {
        try {
            CategoryEventDto event = objectMapper.readValue(rawMessage, CategoryEventDto.class);
            log.info("Received category event | categoryId={}", event.getPayload().getCategoryId());
            catalogCategoryService.upsertCategory(event.getPayload());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process category event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}