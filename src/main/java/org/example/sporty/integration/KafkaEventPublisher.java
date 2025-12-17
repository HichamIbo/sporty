package org.example.sporty.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sporty.domain.model.ScoreData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Publisher for sending event score updates to Kafka.
 *
 * Handles serialization and publishing of messages with proper error handling and retry logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.score-updates}")
    private String scoreUpdatesTopic;

    @Value("${kafka.publish.timeout:5000}")
    private long publishTimeoutMillis;

    /**
     * Publishes a score update to Kafka.
     *
     * @param scoreData the score data to publish
     */
    public void publishScoreUpdate(ScoreData scoreData) {
        String eventId = scoreData.getEventId();

        try {
            String message = serializeScoreData(scoreData);

            log.debug("Publishing score update for event {} to topic {}", eventId, scoreUpdatesTopic);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    scoreUpdatesTopic,
                    eventId,  // Use eventId as the message key for partitioning
                    message
            );

            // Add callback handlers
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccess(result, eventId);
                } else {
                    handleFailure(ex, eventId);
                }
            });

            // Optionally wait for the send to complete (with timeout)
            // This is synchronous but ensures we know if publishing failed
            try {
                future.get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error("Timeout or error waiting for Kafka publish for event {}: {}",
                        eventId, e.getMessage());
                throw new RuntimeException("Failed to publish message for event " + eventId, e);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize score data for event {}: {}", eventId, e.getMessage(), e);
            throw new RuntimeException("Serialization error for event " + eventId, e);
        }
    }

    /**
     * Serializes score data to JSON string.
     */
    private String serializeScoreData(ScoreData scoreData) throws JsonProcessingException {
        return objectMapper.writeValueAsString(scoreData);
    }

    /**
     * Handles successful message publishing.
     */
    private void handleSuccess(SendResult<String, String> result, String eventId) {
        var metadata = result.getRecordMetadata();
        log.info("Successfully published score update for event {} to topic {} (partition: {}, offset: {})",
                eventId,
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
    }

    /**
     * Handles failed message publishing.
     */
    private void handleFailure(Throwable ex, String eventId) {
        log.error("Failed to publish score update for event {}: {}",
                eventId, ex.getMessage(), ex);
    }
}

