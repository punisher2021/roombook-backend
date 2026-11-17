package com.meetingrooms.booking.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(Long roomId) {
        super("La sala de reuniones con id " + roomId + " no se encuentra.");
    }
}
