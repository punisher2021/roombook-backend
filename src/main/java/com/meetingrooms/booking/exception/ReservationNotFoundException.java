package com.meetingrooms.booking.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long reservationId) {
        super("La reservación con id " + reservationId + " no se encuentra.");
    }
}
