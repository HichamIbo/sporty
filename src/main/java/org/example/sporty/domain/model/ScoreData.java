package org.example.sporty.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents the score data from external API response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreData {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("currentScore")
    private String currentScore;

    /**
     * Timestamp when the data was fetched (added by our service)
     */
    private Instant timestamp;
}

