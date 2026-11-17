package com.meetingrooms.booking.dto.response;

import com.meetingrooms.booking.domain.entity.Room;

import java.time.Instant;

public record RoomResponse(
        Long id,
        String name,
        Integer capacity,
        String location,
        Instant createdAt
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getLocation(),
                room.getCreatedAt()
        );
    }
}
