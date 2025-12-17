package org.example.sporty.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sporty.domain.model.Event;
import org.example.sporty.domain.model.EventStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing sports events and their lifecycle.
 *
 * This service maintains the in-memory state of events and coordinates
 * with the scheduler service to start/stop monitoring tasks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventManagementService {

    private final EventSchedulerService schedulerService;

    /**
     * Thread-safe in-memory storage for events.
     */
    private final Map<String, Event> events = new ConcurrentHashMap<>();

    /**
     * Updates the status of an event and triggers appropriate scheduling actions.
     *
     * @param eventId the event ID
     * @param newStatus the new status
     * @return the updated event
     */
    public Event updateEventStatus(String eventId, EventStatus newStatus) {
        log.info("Updating event {} to status: {}", eventId, newStatus);

        Event event = events.computeIfAbsent(eventId, id -> Event.builder()
                .eventId(id)
                .build());

        EventStatus previousStatus = event.getStatus();
        event.setStatus(newStatus);
        event.setLastUpdated(Instant.now());

        handleStatusChange(event, previousStatus, newStatus);

        return event;
    }

    /**
     * Retrieves an event by ID.
     *
     * @param eventId the event ID
     * @return optional containing the event if found
     */
    public Optional<Event> getEvent(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    /**
     * Gets all events currently in the system.
     *
     * @return map of event ID to event
     */
    public Map<String, Event> getAllEvents() {
        return new ConcurrentHashMap<>(events);
    }

    /**
     * Handles status transitions and triggers appropriate actions.
     */
    private void handleStatusChange(Event event, EventStatus previousStatus, EventStatus newStatus) {
        String eventId = event.getEventId();

        // If transitioning to live, start scheduling
        if (newStatus.isLive() && (previousStatus == null || !previousStatus.isLive())) {
            log.info("Event {} transitioning to LIVE - starting periodic updates", eventId);
            schedulerService.scheduleEvent(eventId);
        }
        // If transitioning from live to not live, stop scheduling
        else if (!newStatus.isLive() && previousStatus != null && previousStatus.isLive()) {
            log.info("Event {} transitioning to NOT_LIVE - stopping periodic updates", eventId);
            schedulerService.unscheduleEvent(eventId);
        }
    }
}

