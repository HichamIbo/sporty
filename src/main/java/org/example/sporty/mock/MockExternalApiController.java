package org.example.sporty.mock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

/**
 * Mock External API Controller for testing purposes.
 *
 * This controller simulates an external sports score API by generating
 * random scores for events. It can be enabled/disabled via application properties.
 */
@Slf4j
@RestController
@RequestMapping("/events")
@ConditionalOnProperty(name = "mock.external-api.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Mock External API", description = "Mock API simulating external sports score provider (for testing)")
public class MockExternalApiController {

    private final Random random = new Random();

    /**
     * DTO for mock score response (matches the expected external API format).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Score data returned by the external API")
    public static class MockScoreResponse {
        @Schema(description = "Event ID", example = "event-123")
        private String eventId;

        @Schema(description = "Current score in format 'home:away'", example = "2:1")
        private String currentScore;
    }

    /**
     * Mock endpoint that returns random scores for an event.
     *
     * @param eventId the event ID
     * @return mock score data
     */
    @Operation(
            summary = "Get event score (Mock)",
            description = "Mock endpoint that returns randomly generated scores for testing. " +
                    "The score changes on each call to simulate live updates."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Score retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MockScoreResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            )
    })
    @GetMapping("/{eventId}/score")
    public ResponseEntity<MockScoreResponse> getEventScore(
            @Parameter(description = "Event ID to get score for", required = true, example = "event-123")
            @PathVariable String eventId) {

        log.info("Mock API: Received request for event score: {}", eventId);

        // Generate random scores between 0 and 5 for each team
        int homeScore = random.nextInt(6);
        int awayScore = random.nextInt(6);
        String score = homeScore + ":" + awayScore;

        MockScoreResponse response = MockScoreResponse.builder()
                .eventId(eventId)
                .currentScore(score)
                .build();

        log.info("Mock API: Returning score for event {}: {}", eventId, score);

        return ResponseEntity.ok(response);
    }

    /**
     * Optional: Endpoint to simulate API errors for testing error handling.
     */
    @Operation(
            summary = "Simulate API error (Mock)",
            description = "Mock endpoint that simulates an API error for testing error handling"
    )
    @GetMapping("/{eventId}/score/error")
    public ResponseEntity<String> simulateError(
            @Parameter(description = "Event ID", required = true)
            @PathVariable String eventId) {

        log.warn("Mock API: Simulating error for event: {}", eventId);
        return ResponseEntity.status(500).body("Simulated error from external API");
    }

    /**
     * Health check endpoint for the mock API.
     */
    @Operation(
            summary = "Mock API health check",
            description = "Endpoint to verify the mock API is running"
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Mock External API is running");
    }
}

