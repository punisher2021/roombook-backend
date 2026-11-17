package com.meetingrooms.booking.dto.response;

import com.meetingrooms.booking.domain.entity.Reservation;
import com.meetingrooms.booking.domain.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long roomId,
        String roomName,
        String responsible,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        Instant createdAt,
        Instant cancelledAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getRoom().getId(),
                reservation.getRoom().getName(),
                reservation.getResponsible(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getCancelledAt()
        );
    }
}
