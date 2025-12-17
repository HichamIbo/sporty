package org.example.sporty.integration;

import lombok.extern.slf4j.Slf4j;
import org.example.sporty.domain.model.ScoreData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Client for calling external REST APIs to fetch event score data.
 *
 * Uses WebClient for non-blocking HTTP calls with retry logic and timeout handling.
 */
@Slf4j
@Component
public class ExternalApiClient {

    private final WebClient webClient;
    private final String apiBaseUrl;

    @Value("${external.api.timeout:5000}")
    private int timeoutMillis;

    @Value("${external.api.max-retries:2}")
    private int maxRetries;

    public ExternalApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${external.api.base-url}") String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.webClient = webClientBuilder
                .baseUrl(apiBaseUrl)
                .build();

        log.info("ExternalApiClient initialized with base URL: {}", apiBaseUrl);
    }

    /**
     * Fetches the current score for an event from the external API.
     *
     * @param eventId the event ID
     * @return Mono containing the score data
     */
    public Mono<ScoreData> fetchEventScore(String eventId) {
        log.debug("Calling external API for event: {}", eventId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/events/{eventId}/score")
                        .build(eventId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ScoreData.class)
                .timeout(Duration.ofMillis(timeoutMillis))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(500))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying external API call for event {} (attempt {})",
                                        eventId, retrySignal.totalRetries() + 1)))
                .doOnSuccess(data ->
                        log.debug("Successfully fetched data from external API for event: {}", eventId))
                .doOnError(error ->
                        log.error("Failed to fetch data from external API for event {}: {}",
                                eventId, error.getMessage()));
    }

    /**
     * Determines if an exception should trigger a retry.
     */
    private boolean isRetryableException(Throwable throwable) {
        // Retry on network errors and 5xx server errors
        return throwable instanceof WebClientException ||
               throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }
}

