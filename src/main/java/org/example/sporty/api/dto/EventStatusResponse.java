package org.example.sporty.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.sporty.domain.model.EventStatus;

import java.time.Instant;

/**
 * Response DTO for event status operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing event status information")
public class EventStatusResponse {

    @JsonProperty("eventId")
    @Schema(description = "Unique identifier for the sports event", example = "event-123")
    private String eventId;

    @JsonProperty("status")
    @Schema(description = "Current status of the event", example = "live")
    private EventStatus status;

    @JsonProperty("lastUpdated")
    @Schema(description = "Timestamp when the event status was last updated", example = "2025-12-17T10:30:00Z")
    private Instant lastUpdated;

    @JsonProperty("message")
    @Schema(description = "Human-readable status message", example = "Event is now live and being tracked")
    private String message;
}

