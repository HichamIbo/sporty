package org.example.sporty.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sporty.domain.model.ScoreData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KafkaEventPublisher.
 */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SendResult<String, String> sendResult;

    @InjectMocks
    private KafkaEventPublisher kafkaPublisher;

    private static final String TEST_TOPIC = "test-topic";
    private static final String TEST_EVENT_ID = "event-123";

    @Test
    void publishScoreUpdate_Success_ShouldPublishMessage() throws Exception {
        // Given
        ReflectionTestUtils.setField(kafkaPublisher, "scoreUpdatesTopic", TEST_TOPIC);
        ReflectionTestUtils.setField(kafkaPublisher, "publishTimeoutMillis", 5000L);

        ScoreData scoreData = ScoreData.builder()
                .eventId(TEST_EVENT_ID)
                .currentScore("2:1")
                .timestamp(Instant.now())
                .build();

        String jsonMessage = "{\"eventId\":\"event-123\",\"currentScore\":\"2:1\"}";
        when(objectMapper.writeValueAsString(scoreData)).thenReturn(jsonMessage);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When
        kafkaPublisher.publishScoreUpdate(scoreData);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(1)).send(
                topicCaptor.capture(),
                keyCaptor.capture(),
                messageCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo(TEST_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo(TEST_EVENT_ID);
        assertThat(messageCaptor.getValue()).isEqualTo(jsonMessage);
    }

    @Test
    void publishScoreUpdate_WhenSerializationFails_ShouldThrowException() throws Exception {
        // Given
        ReflectionTestUtils.setField(kafkaPublisher, "scoreUpdatesTopic", TEST_TOPIC);

        ScoreData scoreData = ScoreData.builder()
                .eventId(TEST_EVENT_ID)
                .currentScore("2:1")
                .build();

        when(objectMapper.writeValueAsString(scoreData))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Serialization error") {});

        // When/Then
        assertThatThrownBy(() -> kafkaPublisher.publishScoreUpdate(scoreData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Serialization error");

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publishScoreUpdate_WhenKafkaFails_ShouldThrowException() throws Exception {
        // Given
        ReflectionTestUtils.setField(kafkaPublisher, "scoreUpdatesTopic", TEST_TOPIC);
        ReflectionTestUtils.setField(kafkaPublisher, "publishTimeoutMillis", 1000L);

        ScoreData scoreData = ScoreData.builder()
                .eventId(TEST_EVENT_ID)
                .currentScore("2:1")
                .build();

        String jsonMessage = "{\"eventId\":\"event-123\"}";
        when(objectMapper.writeValueAsString(scoreData)).thenReturn(jsonMessage);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When/Then
        assertThatThrownBy(() -> kafkaPublisher.publishScoreUpdate(scoreData))
                .isInstanceOf(RuntimeException.class);

        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
    }
}

