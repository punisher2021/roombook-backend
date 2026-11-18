package com.meetingrooms.booking.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingrooms.booking.domain.entity.Room;
import com.meetingrooms.booking.domain.repository.ReservationRepository;
import com.meetingrooms.booking.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Room room;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        room = roomRepository.save(new Room("Sala Kepler", 14, "Planta 4"));
    }

    @Test
    void shouldCreateDetectOverlap409FilterAndCancelReservationEndToEnd() throws Exception {
        LocalDate targetDate = LocalDate.now().plusDays(5);
        LocalDateTime start = targetDate.atTime(10, 0);
        LocalDateTime end = targetDate.atTime(11, 30);

        String createBody = """
                {
                  "roomId": %d,
                  "responsible": "Marta Sánchez",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(room.getId(), start, end);

        // 1. Unauthenticated POST -> 401
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isUnauthorized());

        // 2. Authenticated POST -> 201 Created
        MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(room.getId()))
                .andExpect(jsonPath("$.responsible").value("Marta Sánchez"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long reservationId = createdJson.get("id").asLong();

        // 3. Overlapping slot [11:00, 12:00) -> 409 Conflict
        String overlappingBody = """
                {
                  "roomId": %d,
                  "responsible": "Pedro López",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(room.getId(), targetDate.atTime(11, 0), targetDate.atTime(12, 0));

        mockMvc.perform(post("/api/v1/reservations")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_TIME_CONFLICT"));

        // 4. Back-to-back slot [11:30, 12:30) -> 201 Created
        String adjacentBody = """
                {
                  "roomId": %d,
                  "responsible": "Pedro López",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(room.getId(), targetDate.atTime(11, 30), targetDate.atTime(12, 30));

        mockMvc.perform(post("/api/v1/reservations")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjacentBody))
                .andExpect(status().isCreated());

        // 5. Public GET with roomId + date filter + pagination -> 200 OK
        mockMvc.perform(get("/api/v1/reservations")
                        .param("roomId", room.getId().toString())
                        .param("date", targetDate.toString())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].responsible").value("Marta Sánchez"));

        // 6. Unauthenticated DELETE -> 401
        mockMvc.perform(delete("/api/v1/reservations/{id}", reservationId))
                .andExpect(status().isUnauthorized());

        // 7. Authenticated DELETE -> 200 CANCELLED
        mockMvc.perform(delete("/api/v1/reservations/{id}", reservationId)
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 8. Original slot is free again after cancellation -> 201 Created
        mockMvc.perform(post("/api/v1/reservations")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn400ForValidationErrorsAndInvalidTimeRange() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        String badPayload = """
                {
                  "roomId": %d,
                  "responsible": "   ",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(room.getId(), past, past.plusHours(1));

        mockMvc.perform(post("/api/v1/reservations")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.responsible").exists())
                .andExpect(jsonPath("$.fieldErrors.startTime").exists());

        LocalDateTime futureStart = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);
        String reversedRange = """
                {
                  "roomId": %d,
                  "responsible": "Lucía Martín",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(room.getId(), futureStart, futureStart.minusHours(1));

        mockMvc.perform(post("/api/v1/reservations")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reversedRange))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }
}
