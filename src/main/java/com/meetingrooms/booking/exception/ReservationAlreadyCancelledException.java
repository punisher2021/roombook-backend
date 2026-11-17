package com.meetingrooms.booking.exception;

public class ReservationAlreadyCancelledException extends RuntimeException {
    public ReservationAlreadyCancelledException(Long reservationId) {
        super("La reservación " + reservationId + " ha sido cancelada.");
    }
}
