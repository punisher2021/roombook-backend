package com.meetingrooms.booking.service;

import com.meetingrooms.booking.domain.entity.Room;
import com.meetingrooms.booking.domain.repository.RoomRepository;
import com.meetingrooms.booking.dto.request.CreateRoomRequest;
import com.meetingrooms.booking.dto.response.PageResponse;
import com.meetingrooms.booking.dto.response.RoomResponse;
import com.meetingrooms.booking.exception.DuplicateRoomException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        String normalizedName = request.name().trim();
        if (roomRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateRoomException(normalizedName);
        }

        Room room = new Room(
                normalizedName,
                request.capacity(),
                request.location().trim()
        );
        Room saved = roomRepository.save(room);
        return RoomResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> listRooms(Pageable pageable) {
        Page<RoomResponse> page = roomRepository.findAll(pageable).map(RoomResponse::from);
        return PageResponse.from(page);
    }
}
