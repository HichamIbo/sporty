package org.example.sporty.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sporty.domain.model.ScoreData;
import org.example.sporty.integration.ExternalApiClient;
import org.example.sporty.integration.KafkaEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service responsible for fetching event data from external APIs
 * and publishing to Kafka.
 *
 * This service orchestrates the data flow from external sources to the message broker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventDataFetchService {

    private final ExternalApiClient externalApiClient;
    private final KafkaEventPublisher kafkaPublisher;

    /**
     * Fetches event data from the external API and publishes it to Kafka.
     *
     * @param eventId the event ID
     */
    public void fetchAndPublishEventData(String eventId) {
        log.debug("Fetching data for event: {}", eventId);

        try {
            // Fetch data from external API
            ScoreData scoreData = externalApiClient.fetchEventScore(eventId)
                    .block(); // Block to convert from Mono to synchronous call

            if (scoreData == null) {
                log.warn("No data received from external API for event: {}", eventId);
                return;
            }

            // Enrich with timestamp if not present
            if (scoreData.getTimestamp() == null) {
                scoreData.setTimestamp(Instant.now());
            }

            log.info("Fetched score data for event {}: {}", eventId, scoreData.getCurrentScore());

            // Publish to Kafka
            kafkaPublisher.publishScoreUpdate(scoreData);

        } catch (Exception e) {
            log.error("Failed to fetch and publish data for event {}: {}",
                    eventId, e.getMessage(), e);
            throw new RuntimeException("Error processing event " + eventId, e);
        }
    }
}

