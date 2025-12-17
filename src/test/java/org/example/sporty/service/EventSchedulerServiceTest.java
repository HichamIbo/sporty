package org.example.sporty.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventSchedulerService.
 */
@ExtendWith(MockitoExtension.class)
class EventSchedulerServiceTest {

    @Mock
    private EventDataFetchService dataFetchService;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @InjectMocks
    private EventSchedulerService schedulerService;

    private static final String TEST_EVENT_ID = "event-123";

    @BeforeEach
    void setUp() {
        lenient().when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class)))
                .thenAnswer(invocation -> scheduledFuture);
        lenient().when(scheduledFuture.isCancelled()).thenReturn(false);
        lenient().when(scheduledFuture.isDone()).thenReturn(false);
    }


    @Test
    void scheduleEvent_ShouldCreateScheduledTask() {
        // When
        schedulerService.scheduleEvent(TEST_EVENT_ID);

        // Then
        verify(taskScheduler, times(1)).scheduleAtFixedRate(
                any(Runnable.class),
                eq(Duration.ofSeconds(10))
        );
        assertThat(schedulerService.isScheduled(TEST_EVENT_ID)).isTrue();
    }

    @Test
    void scheduleEvent_WhenAlreadyScheduled_ShouldCancelPreviousTask() {
        // Given
        schedulerService.scheduleEvent(TEST_EVENT_ID);

        // When - Schedule again
        schedulerService.scheduleEvent(TEST_EVENT_ID);

        // Then
        verify(scheduledFuture, times(1)).cancel(false);
        verify(taskScheduler, times(2)).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    void unscheduleEvent_WhenScheduled_ShouldCancelTask() {
        // Given
        schedulerService.scheduleEvent(TEST_EVENT_ID);

        // When
        schedulerService.unscheduleEvent(TEST_EVENT_ID);

        // Then
        verify(scheduledFuture, times(1)).cancel(false);
        assertThat(schedulerService.getScheduledEventCount()).isEqualTo(0);
    }

    @Test
    void unscheduleEvent_WhenNotScheduled_ShouldNotThrowException() {
        // When/Then - Should not throw exception
        schedulerService.unscheduleEvent("non-existent-event");

        verify(scheduledFuture, never()).cancel(anyBoolean());
    }

    @Test
    void isScheduled_WhenTaskIsCancelled_ShouldReturnFalse() {
        // Given
        schedulerService.scheduleEvent(TEST_EVENT_ID);
        when(scheduledFuture.isCancelled()).thenReturn(true);

        // When/Then
        assertThat(schedulerService.isScheduled(TEST_EVENT_ID)).isFalse();
    }

    @Test
    void isScheduled_WhenTaskIsDone_ShouldReturnFalse() {
        // Given
        schedulerService.scheduleEvent(TEST_EVENT_ID);
        when(scheduledFuture.isDone()).thenReturn(true);

        // When/Then
        assertThat(schedulerService.isScheduled(TEST_EVENT_ID)).isFalse();
    }

    @Test
    void getScheduledEventCount_ShouldReturnCorrectCount() {
        // Given
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class)))
                .thenAnswer(invocation -> scheduledFuture);

        // When
        schedulerService.scheduleEvent("event-1");
        schedulerService.scheduleEvent("event-2");
        schedulerService.scheduleEvent("event-3");

        // Then
        assertThat(schedulerService.getScheduledEventCount()).isEqualTo(3);
    }

    @Test
    void scheduledTask_WhenExecuted_ShouldCallDataFetchService() {
        // This test verifies the actual task execution would work
        // In a real scheduler, we would use integration tests
        doNothing().when(dataFetchService).fetchAndPublishEventData(anyString());

        // Create a real scheduler for this test
        ThreadPoolTaskScheduler realScheduler = new ThreadPoolTaskScheduler();
        realScheduler.setPoolSize(1);
        realScheduler.setThreadNamePrefix("test-");
        realScheduler.initialize();

        EventSchedulerService realSchedulerService = new EventSchedulerService(
                dataFetchService,
                realScheduler
        );

        // When
        realSchedulerService.scheduleEvent(TEST_EVENT_ID);

        // Then - Wait for at least one execution
        await().atMost(Duration.ofSeconds(12))
                .untilAsserted(() -> verify(dataFetchService, atLeastOnce())
                        .fetchAndPublishEventData(TEST_EVENT_ID));

        // Cleanup
        realSchedulerService.unscheduleEvent(TEST_EVENT_ID);
        realScheduler.shutdown();
    }
}

