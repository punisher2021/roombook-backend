package com.meetingrooms.booking.exception;

import java.time.LocalDateTime;

public class RoomConflictException extends RuntimeException {
    public RoomConflictException(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        super("La habitación " + roomId + " está ocupada entre " + startTime + " y " + endTime);
    }
}
