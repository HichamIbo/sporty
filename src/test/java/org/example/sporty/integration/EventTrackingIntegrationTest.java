package org.example.sporty.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.sporty.api.dto.EventStatusRequest;
import org.example.sporty.domain.model.EventStatus;
import org.example.sporty.domain.model.ScoreData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the complete event tracking flow.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
        partitions = 1,
        topics = {"${kafka.topic.score-updates}"}
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventTrackingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topic.score-updates}")
    private String scoreUpdatesTopic;

    private BlockingQueue<ConsumerRecord<String, String>> records;
    private KafkaMessageListenerContainer<String, String> container;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProps = new ContainerProperties(scoreUpdatesTopic);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
        container.setupMessageListener((MessageListener<String, String>) records::add);
        container.start();

        // Give the container some time to start and subscribe
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clear any messages that may have been produced before this test started
        records.clear();
        log.info("Test setup complete. Kafka consumer ready for new messages.");
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void completeFlow_WhenEventSetToLive_ShouldPublishScoreUpdates() throws Exception {
        // Given
        String eventId = "integration-test-event-1";
        EventStatusRequest request = EventStatusRequest.builder()
                .eventId(eventId)
                .status(EventStatus.LIVE)
                .build();

        // When - Set event to LIVE (this triggers scheduling and periodic Kafka publishing)
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/events/status",
                request,
                String.class
        );

        // Then - Verify HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(eventId);
        assertThat(response.getBody()).contains("live");

        // Then - Verify Kafka message was published
        // The scheduler runs every 10 seconds, so we wait up to 15 seconds for at least one message
        // Poll messages until we find one for our event (or timeout)
        ConsumerRecord<String, String> record = null;
        long endTime = System.currentTimeMillis() + 15000; // 15 seconds timeout

        while (System.currentTimeMillis() < endTime && record == null) {
            ConsumerRecord<String, String> polledRecord = records.poll(2, TimeUnit.SECONDS);
            if (polledRecord != null && eventId.equals(polledRecord.key())) {
                record = polledRecord;
                break;
            }
        }

        assertThat(record)
                .as("Expected a Kafka message to be published to topic " + scoreUpdatesTopic + " for event " + eventId)
                .isNotNull();

        // Verify the message is on the correct topic
        assertThat(record.topic()).isEqualTo(scoreUpdatesTopic);

        // Verify the message key is the event ID
        assertThat(record.key()).isEqualTo(eventId);

        // Verify the message payload contains the event ID
        assertThat(record.value()).contains(eventId);

        // Optional: Deserialize and verify the ScoreData structure
        ScoreData scoreData = objectMapper.readValue(record.value(), ScoreData.class);
        assertThat(scoreData.getEventId()).isEqualTo(eventId);
        assertThat(scoreData.getCurrentScore()).isNotNull();
        assertThat(scoreData.getTimestamp()).isNotNull();

        log.info("Successfully verified Kafka message: topic={}, key={}, value={}",
                record.topic(), record.key(), record.value());
    }

    @Test
    void eventStatusLifecycle_ShouldTransitionCorrectly() throws Exception {
        // Given
        String eventId = "lifecycle-test-event";

        // When - Set to live
        EventStatusRequest liveRequest = EventStatusRequest.builder()
                .eventId(eventId)
                .status(EventStatus.LIVE)
                .build();

        ResponseEntity<String> liveResponse = restTemplate.postForEntity(
                "/api/events/status",
                liveRequest,
                String.class
        );

        // Then - Verify HTTP response
        assertThat(liveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Then - Verify Kafka messages are being published
        // Poll messages until we find one for our event (or timeout)
        ConsumerRecord<String, String> firstMessage = null;
        long endTime = System.currentTimeMillis() + 15000; // 15 seconds timeout

        while (System.currentTimeMillis() < endTime && firstMessage == null) {
            ConsumerRecord<String, String> polledRecord = records.poll(2, TimeUnit.SECONDS);
            if (polledRecord != null && eventId.equals(polledRecord.key())) {
                firstMessage = polledRecord;
                break;
            }
        }

        assertThat(firstMessage)
                .as("Expected Kafka messages when event is LIVE")
                .isNotNull();
        assertThat(firstMessage.key()).isEqualTo(eventId);
        log.info("Received first Kafka message while LIVE: {}", firstMessage.value());

        // When - Fetch status
        ResponseEntity<String> statusResponse = restTemplate.getForEntity(
                "/api/events/" + eventId + "/status",
                String.class
        );

        // Then
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody()).contains("live");

        // When - Set to not live
        EventStatusRequest notLiveRequest = EventStatusRequest.builder()
                .eventId(eventId)
                .status(EventStatus.NOT_LIVE)
                .build();

        ResponseEntity<String> notLiveResponse = restTemplate.postForEntity(
                "/api/events/status",
                notLiveRequest,
                String.class
        );

        // Then
        assertThat(notLiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notLiveResponse.getBody()).contains("not_live");

        // Then - Verify no more messages are published after setting to NOT_LIVE
        // Clear any remaining messages
        records.clear();

        // Wait 12 seconds (longer than the 10-second schedule interval)
        // and verify no new messages are published for this event
        long stopTime = System.currentTimeMillis() + 12000;
        boolean foundMessageAfterStop = false;

        while (System.currentTimeMillis() < stopTime) {
            ConsumerRecord<String, String> polledRecord = records.poll(2, TimeUnit.SECONDS);
            if (polledRecord != null && eventId.equals(polledRecord.key())) {
                foundMessageAfterStop = true;
                break;
            }
        }

        assertThat(foundMessageAfterStop)
                .as("Expected no Kafka messages for event " + eventId + " when it is NOT_LIVE")
                .isFalse();

        log.info("Verified that no Kafka messages are published when event is NOT_LIVE");
    }

    @Test
    void updateEventStatus_WithInvalidData_ShouldReturnBadRequest() {
        // Given
        EventStatusRequest invalidRequest = EventStatusRequest.builder()
                .status(EventStatus.LIVE)
                // Missing eventId
                .build();

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/events/status",
                invalidRequest,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation Failed");
    }
}

