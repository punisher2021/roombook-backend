package com.meetingrooms.booking.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateReservationRequest(
        @NotNull(message = "La habitación es requerida")
        @Positive(message = "El id de la habitación debe ser un número positivo")
        Long roomId,

        @NotBlank(message = "El responsable es requerido")
        @Size(max = 120, message = "El responsable no debe exeder 120 characters")
        String responsible,

        @NotNull(message = "La fecha de inicio es requerida")
        @Future(message = "La fecha de inicio debe ser superior a la fecha actual")
        LocalDateTime startTime,

        @NotNull(message = "La fecha fin es requerida")
        @Future(message = "La fecha fin debe ser superior a la fecha actual")
        LocalDateTime endTime
) {
}
