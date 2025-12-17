package org.example.sporty.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service responsible for scheduling and managing periodic event update tasks.
 *
 * Uses Spring's TaskScheduler to dynamically schedule tasks for each live event.
 * Each event gets its own scheduled task that can be independently started and stopped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventSchedulerService {

    private final EventDataFetchService dataFetchService;
    private final ThreadPoolTaskScheduler taskScheduler;

    /**
     * Stores scheduled futures for each event to allow cancellation.
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Interval between successive calls to the external API (10 seconds).
     */
    private static final Duration FETCH_INTERVAL = Duration.ofSeconds(10);

    /**
     * Schedules periodic updates for an event.
     *
     * @param eventId the event ID to schedule
     */
    public void scheduleEvent(String eventId) {
        // Cancel existing task if any
        unscheduleEvent(eventId);

        log.info("Scheduling periodic updates for event: {} (every {} seconds)",
                eventId, FETCH_INTERVAL.getSeconds());

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(
                () -> fetchAndPublishEventData(eventId),
                FETCH_INTERVAL
        );

        scheduledTasks.put(eventId, future);
        log.debug("Scheduled task created for event: {}", eventId);
    }

    /**
     * Unschedules periodic updates for an event.
     *
     * @param eventId the event ID to unschedule
     */
    public void unscheduleEvent(String eventId) {
        ScheduledFuture<?> future = scheduledTasks.remove(eventId);

        if (future != null) {
            boolean cancelled = future.cancel(false);
            log.info("Unscheduled periodic updates for event: {} (cancelled: {})", eventId, cancelled);
        } else {
            log.debug("No scheduled task found for event: {}", eventId);
        }
    }

    /**
     * Checks if an event is currently scheduled.
     *
     * @param eventId the event ID
     * @return true if the event has an active scheduled task
     */
    public boolean isScheduled(String eventId) {
        ScheduledFuture<?> future = scheduledTasks.get(eventId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    /**
     * Fetches data from external API and publishes to Kafka.
     * This is the task that runs periodically for each live event.
     *
     * @param eventId the event ID
     */
    private void fetchAndPublishEventData(String eventId) {
        try {
            log.debug("Executing scheduled fetch for event: {}", eventId);
            dataFetchService.fetchAndPublishEventData(eventId);
        } catch (Exception e) {
            // Log the error but don't propagate - we want the task to continue
            log.error("Error in scheduled task for event {}: {}", eventId, e.getMessage(), e);
        }
    }

    /**
     * Gets the count of currently scheduled events.
     *
     * @return number of active scheduled tasks
     */
    public int getScheduledEventCount() {
        return (int) scheduledTasks.values().stream()
                .filter(future -> !future.isCancelled() && !future.isDone())
                .count();
    }
}

