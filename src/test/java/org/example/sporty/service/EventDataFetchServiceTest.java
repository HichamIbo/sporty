package org.example.sporty.service;

import org.example.sporty.domain.model.ScoreData;
import org.example.sporty.integration.ExternalApiClient;
import org.example.sporty.integration.KafkaEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventDataFetchService.
 */
@ExtendWith(MockitoExtension.class)
class EventDataFetchServiceTest {

    @Mock
    private ExternalApiClient externalApiClient;

    @Mock
    private KafkaEventPublisher kafkaPublisher;

    @InjectMocks
    private EventDataFetchService dataFetchService;

    private static final String TEST_EVENT_ID = "event-123";

    @Test
    void fetchAndPublishEventData_Success_ShouldFetchAndPublish() {
        // Given
        ScoreData scoreData = ScoreData.builder()
                .eventId(TEST_EVENT_ID)
                .currentScore("2:1")
                .build();

        when(externalApiClient.fetchEventScore(TEST_EVENT_ID))
                .thenReturn(Mono.just(scoreData));

        // When
        dataFetchService.fetchAndPublishEventData(TEST_EVENT_ID);

        // Then
        verify(externalApiClient, times(1)).fetchEventScore(TEST_EVENT_ID);

        ArgumentCaptor<ScoreData> captor = ArgumentCaptor.forClass(ScoreData.class);
        verify(kafkaPublisher, times(1)).publishScoreUpdate(captor.capture());

        ScoreData publishedData = captor.getValue();
        assertThat(publishedData.getEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(publishedData.getCurrentScore()).isEqualTo("2:1");
        assertThat(publishedData.getTimestamp()).isNotNull();
    }

    @Test
    void fetchAndPublishEventData_WhenDataHasTimestamp_ShouldNotOverwrite() {
        // Given
        Instant originalTimestamp = Instant.now().minusSeconds(5);
        ScoreData scoreData = ScoreData.builder()
                .eventId(TEST_EVENT_ID)
                .currentScore("3:0")
                .timestamp(originalTimestamp)
                .build();

        when(externalApiClient.fetchEventScore(TEST_EVENT_ID))
                .thenReturn(Mono.just(scoreData));

        // When
        dataFetchService.fetchAndPublishEventData(TEST_EVENT_ID);

        // Then
        ArgumentCaptor<ScoreData> captor = ArgumentCaptor.forClass(ScoreData.class);
        verify(kafkaPublisher, times(1)).publishScoreUpdate(captor.capture());

        ScoreData publishedData = captor.getValue();
        assertThat(publishedData.getTimestamp()).isEqualTo(originalTimestamp);
    }

    @Test
    void fetchAndPublishEventData_WhenApiReturnsNull_ShouldNotPublish() {
        // Given
        when(externalApiClient.fetchEventScore(TEST_EVENT_ID))
                .thenReturn(Mono.empty());

        // When
        dataFetchService.fetchAndPublishEventData(TEST_EVENT_ID);

        // Then
        verify(externalApiClient, times(1)).fetchEventScore(TEST_EVENT_ID);
        verify(kafkaPublisher, never()).publishScoreUpdate(any());
    }

    @Test
    void fetchAndPublishEventData_WhenApiFails_ShouldThrowException() {
        // Given
        when(externalApiClient.fetchEventScore(TEST_EVENT_ID))
                .thenReturn(Mono.error(new RuntimeException("API error")));

        // When/Then
        assertThatThrownBy(() -> dataFetchService.fetchAndPublishEventData(TEST_EVENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error processing event");

        verify(kafkaPublisher, never()).publishScoreUpdate(any());
    }

    @Test
    void fetchAndPublishEventData_WhenPublishFails_ShouldThrowException() {
        // Given
        ScoreData scoreData = ScoreData.builder()
                .eventId(TEST_EVENT_ID)
                .currentScore("1:1")
                .build();

        when(externalApiClient.fetchEventScore(TEST_EVENT_ID))
                .thenReturn(Mono.just(scoreData));

        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaPublisher).publishScoreUpdate(any());

        // When/Then
        assertThatThrownBy(() -> dataFetchService.fetchAndPublishEventData(TEST_EVENT_ID))
                .isInstanceOf(RuntimeException.class);

        verify(externalApiClient, times(1)).fetchEventScore(TEST_EVENT_ID);
        verify(kafkaPublisher, times(1)).publishScoreUpdate(any());
    }
}

