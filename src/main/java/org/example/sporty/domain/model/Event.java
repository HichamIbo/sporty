package org.example.sporty.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a sports event in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private String eventId;
    private EventStatus status;
    private Instant lastUpdated;

    public boolean isLive() {
        return status != null && status.isLive();
    }
}

