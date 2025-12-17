package org.example.sporty.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error response containing details about the failure")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2025-12-17T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type", example = "Validation Failed")
    private String error;

    @Schema(description = "Detailed error message", example = "Invalid request parameters")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/events/status")
    private String path;

    @Schema(description = "Validation errors by field name")
    private Map<String, String> validationErrors;
}

