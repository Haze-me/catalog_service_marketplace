package com.marketplace.catalog.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.application.service.ProductRatingService;
import com.marketplace.catalog.infrastructure.kafka.dto.ReviewEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final ProductRatingService productRatingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "review.created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleReviewCreated(String rawMessage, Acknowledgment ack) {
        process(rawMessage, ack);
    }

    @KafkaListener(topics = "review.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleReviewUpdated(String rawMessage, Acknowledgment ack) {
        process(rawMessage, ack);
    }

    @KafkaListener(topics = "review.deleted", groupId = "${spring.kafka.consumer.group-id}")
    public void handleReviewDeleted(String rawMessage, Acknowledgment ack) {
        process(rawMessage, ack);
    }

    private void process(String rawMessage, Acknowledgment ack) {
        try {
            ReviewEventDto event = objectMapper.readValue(rawMessage, ReviewEventDto.class);
            log.info("Received review event | productId={}", event.getPayload().getProductId());
            productRatingService.recalculateRating(event.getPayload().getProductId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process review event | rawMessage={} error={}", rawMessage, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}