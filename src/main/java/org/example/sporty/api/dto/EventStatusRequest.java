package org.example.sporty.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.sporty.domain.model.EventStatus;

/**
 * Request DTO for updating event status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating event status")
public class EventStatusRequest {

    @NotBlank(message = "Event ID is required")
    @JsonProperty("eventId")
    @Schema(description = "Unique identifier for the sports event", example = "event-123")
    private String eventId;

    @NotNull(message = "Status is required")
    @JsonProperty("status")
    @Schema(description = "Event status: 'live' to start tracking, 'not_live' to stop",
            example = "live",
            allowableValues = {"live", "not_live"})
    private EventStatus status;
}

