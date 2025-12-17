package org.example.sporty.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sporty.api.dto.EventStatusRequest;
import org.example.sporty.domain.model.Event;
import org.example.sporty.domain.model.EventStatus;
import org.example.sporty.service.EventManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EventController.
 */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventManagementService eventManagementService;

    private static final String TEST_EVENT_ID = "event-123";

    @Test
    void updateEventStatus_ValidRequest_ShouldReturnOk() throws Exception {
        // Given
        EventStatusRequest request = EventStatusRequest.builder()
                .eventId(TEST_EVENT_ID)
                .status(EventStatus.LIVE)
                .build();

        Event event = Event.builder()
                .eventId(TEST_EVENT_ID)
                .status(EventStatus.LIVE)
                .lastUpdated(Instant.now())
                .build();

        when(eventManagementService.updateEventStatus(eq(TEST_EVENT_ID), eq(EventStatus.LIVE)))
                .thenReturn(event);

        // When/Then
        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.status").value("live"))
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updateEventStatus_MissingEventId_ShouldReturnBadRequest() throws Exception {
        // Given
        EventStatusRequest request = EventStatusRequest.builder()
                .status(EventStatus.LIVE)
                .build();

        // When/Then
        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void updateEventStatus_MissingStatus_ShouldReturnBadRequest() throws Exception {
        // Given
        EventStatusRequest request = EventStatusRequest.builder()
                .eventId(TEST_EVENT_ID)
                .build();

        // When/Then
        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void updateEventStatus_InvalidStatus_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidJson = "{\"eventId\":\"" + TEST_EVENT_ID + "\",\"status\":\"invalid_status\"}";

        // When/Then
        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEventStatus_WhenEventExists_ShouldReturnOk() throws Exception {
        // Given
        Event event = Event.builder()
                .eventId(TEST_EVENT_ID)
                .status(EventStatus.LIVE)
                .lastUpdated(Instant.now())
                .build();

        when(eventManagementService.getEvent(TEST_EVENT_ID))
                .thenReturn(Optional.of(event));

        // When/Then
        mockMvc.perform(get("/api/events/{eventId}/status", TEST_EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(TEST_EVENT_ID))
                .andExpect(jsonPath("$.status").value("live"));
    }

    @Test
    void getEventStatus_WhenEventDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Given
        when(eventManagementService.getEvent(TEST_EVENT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/events/{eventId}/status", TEST_EVENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEventStatus_SetToNotLive_ShouldReturnCorrectMessage() throws Exception {
        // Given
        EventStatusRequest request = EventStatusRequest.builder()
                .eventId(TEST_EVENT_ID)
                .status(EventStatus.NOT_LIVE)
                .build();

        Event event = Event.builder()
                .eventId(TEST_EVENT_ID)
                .status(EventStatus.NOT_LIVE)
                .lastUpdated(Instant.now())
                .build();

        when(eventManagementService.updateEventStatus(eq(TEST_EVENT_ID), eq(EventStatus.NOT_LIVE)))
                .thenReturn(event);

        // When/Then
        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_live"))
                .andExpect(jsonPath("$.message").value("Event tracking stopped"));
    }
}

