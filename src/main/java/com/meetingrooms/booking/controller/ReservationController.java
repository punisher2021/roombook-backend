package com.meetingrooms.booking.controller;

import com.meetingrooms.booking.dto.request.CreateReservationRequest;
import com.meetingrooms.booking.dto.response.PageResponse;
import com.meetingrooms.booking.dto.response.ReservationResponse;
import com.meetingrooms.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Endpoints for creating, querying, and cancelling room reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a reservation", security = @SecurityRequirement(name = "basicAuth"))
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationResponse created = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List reservations with optional roomId, date filters and pagination (public)")
    public ResponseEntity<PageResponse<ReservationResponse>> listReservations(
            @RequestParam(required = false) @Positive(message = "roomId must be positive") Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(reservationService.listReservations(roomId, date, includeCancelled, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation details by id (public)")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(reservationService.getReservation(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation", security = @SecurityRequirement(name = "basicAuth"))
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }
}
