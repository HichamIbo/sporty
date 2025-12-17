package org.example.sporty.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sporty.api.dto.ErrorResponse;
import org.example.sporty.api.dto.EventStatusRequest;
import org.example.sporty.api.dto.EventStatusResponse;
import org.example.sporty.domain.model.Event;
import org.example.sporty.service.EventManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing event status updates.
 *
 * Provides endpoints to mark events as live or not live, triggering
 * the appropriate scheduling of periodic data fetching tasks.
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Event Management", description = "APIs for managing live sports event tracking")
public class EventController {

    private final EventManagementService eventManagementService;

    /**
     * Updates the status of a sports event.
     *
     * @param request the event status update request
     * @return response containing the updated event status
     */
    @Operation(
            summary = "Update event status",
            description = "Updates the status of a sports event. When set to 'live', the system will start " +
                    "fetching score updates every 10 seconds. When set to 'not_live', tracking stops."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event status updated successfully",
                    content = @Content(schema = @Schema(implementation = EventStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/status")
    public ResponseEntity<EventStatusResponse> updateEventStatus(
            @Parameter(description = "Event status update request", required = true)
            @Valid @RequestBody EventStatusRequest request) {

        log.info("Received status update request for event: {}, status: {}",
                request.getEventId(), request.getStatus());

        Event event = eventManagementService.updateEventStatus(
                request.getEventId(),
                request.getStatus()
        );

        EventStatusResponse response = EventStatusResponse.builder()
                .eventId(event.getEventId())
                .status(event.getStatus())
                .lastUpdated(event.getLastUpdated())
                .message(buildStatusMessage(event))
                .build();

        log.info("Successfully updated event {} to status: {}",
                event.getEventId(), event.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current status of an event.
     *
     * @param eventId the event ID
     * @return response containing the current event status
     */
    @Operation(
            summary = "Get event status",
            description = "Retrieves the current status of a specific event by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event found",
                    content = @Content(schema = @Schema(implementation = EventStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            )
    })
    @GetMapping("/{eventId}/status")
    public ResponseEntity<EventStatusResponse> getEventStatus(
            @Parameter(description = "ID of the event to retrieve", required = true, example = "event-123")
            @PathVariable String eventId) {

        log.debug("Fetching status for event: {}", eventId);

        return eventManagementService.getEvent(eventId)
                .map(event -> EventStatusResponse.builder()
                        .eventId(event.getEventId())
                        .status(event.getStatus())
                        .lastUpdated(event.getLastUpdated())
                        .message(buildStatusMessage(event))
                        .build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String buildStatusMessage(Event event) {
        if (event.isLive()) {
            return "Event is now live and being tracked";
        } else {
            return "Event tracking stopped";
        }
    }
}

