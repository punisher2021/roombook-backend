package com.meetingrooms.booking.dto.request;

import jakarta.validation.constraints.*;

public record CreateRoomRequest(
        @NotBlank(message = "El nombre de la habitación es requerido")
        @Size(max = 120, message = "El nombre de la habitación no debe exeder 120 characters")
        String name,

        @NotNull(message = "La capacidad de la habitación es requerida")
        @Positive(message = "La capacidad de la habitación debe ser mayor a 0")
        @Max(value = 1000, message = "La capacidad de la habitación no debe ser mayor que 1000")
        Integer capacity,

        @NotBlank(message = "La ubicación de la habitación es requerida")
        @Size(max = 150, message = "La ubicación de la habitación no debe exeder 150 caracteres")
        String location
) {
}
