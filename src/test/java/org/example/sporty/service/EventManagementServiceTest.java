package org.example.sporty.service;

import org.example.sporty.domain.model.Event;
import org.example.sporty.domain.model.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventManagementService.
 */
@ExtendWith(MockitoExtension.class)
class EventManagementServiceTest {

    @Mock
    private EventSchedulerService schedulerService;

    @InjectMocks
    private EventManagementService eventManagementService;

    private static final String TEST_EVENT_ID = "event-123";

    @BeforeEach
    void setUp() {
        // Clear any previous state
    }

    @Test
    void updateEventStatus_WhenNewEventSetToLive_ShouldScheduleEvent() {
        // Given
        EventStatus newStatus = EventStatus.LIVE;

        // When
        Event result = eventManagementService.updateEventStatus(TEST_EVENT_ID, newStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(result.getStatus()).isEqualTo(EventStatus.LIVE);
        assertThat(result.getLastUpdated()).isNotNull();
        assertThat(result.isLive()).isTrue();

        verify(schedulerService, times(1)).scheduleEvent(TEST_EVENT_ID);
        verify(schedulerService, never()).unscheduleEvent(anyString());
    }

    @Test
    void updateEventStatus_WhenEventSetToNotLive_ShouldUnscheduleEvent() {
        // Given - First set to live
        eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.LIVE);
        reset(schedulerService);

        // When - Then set to not live
        Event result = eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.NOT_LIVE);

        // Then
        assertThat(result.getStatus()).isEqualTo(EventStatus.NOT_LIVE);
        assertThat(result.isLive()).isFalse();

        verify(schedulerService, times(1)).unscheduleEvent(TEST_EVENT_ID);
        verify(schedulerService, never()).scheduleEvent(anyString());
    }

    @Test
    void updateEventStatus_WhenAlreadyLiveSetToLiveAgain_ShouldNotReschedule() {
        // Given - Event is already live
        eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.LIVE);
        reset(schedulerService);

        // When - Set to live again
        Event result = eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.LIVE);

        // Then
        assertThat(result.getStatus()).isEqualTo(EventStatus.LIVE);

        // Should not trigger any scheduling changes
        verify(schedulerService, never()).scheduleEvent(anyString());
        verify(schedulerService, never()).unscheduleEvent(anyString());
    }

    @Test
    void getEvent_WhenEventExists_ShouldReturnEvent() {
        // Given
        eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.LIVE);

        // When
        Optional<Event> result = eventManagementService.getEvent(TEST_EVENT_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(result.get().getStatus()).isEqualTo(EventStatus.LIVE);
    }

    @Test
    void getEvent_WhenEventDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Event> result = eventManagementService.getEvent("non-existent-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getAllEvents_ShouldReturnAllStoredEvents() {
        // Given
        eventManagementService.updateEventStatus("event-1", EventStatus.LIVE);
        eventManagementService.updateEventStatus("event-2", EventStatus.NOT_LIVE);
        eventManagementService.updateEventStatus("event-3", EventStatus.LIVE);

        // When
        var allEvents = eventManagementService.getAllEvents();

        // Then
        assertThat(allEvents).hasSize(3);
        assertThat(allEvents.keySet()).containsExactlyInAnyOrder("event-1", "event-2", "event-3");
    }

    @Test
    void updateEventStatus_TransitionFromNotLiveToLive_ShouldSchedule() {
        // Given - Event starts as not live
        eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.NOT_LIVE);
        reset(schedulerService);

        // When - Transition to live
        Event result = eventManagementService.updateEventStatus(TEST_EVENT_ID, EventStatus.LIVE);

        // Then
        assertThat(result.getStatus()).isEqualTo(EventStatus.LIVE);
        verify(schedulerService, times(1)).scheduleEvent(TEST_EVENT_ID);
    }
}

