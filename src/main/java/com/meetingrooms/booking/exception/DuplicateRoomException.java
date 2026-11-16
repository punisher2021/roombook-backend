package com.meetingrooms.booking.exception;

public class DuplicateRoomException extends RuntimeException {
    public DuplicateRoomException(String roomName) {
        super("A room with name '" + roomName + "' already exists");
    }
}
