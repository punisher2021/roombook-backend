package com.meetingrooms.booking.service;

import com.meetingrooms.booking.domain.entity.Reservation;
import com.meetingrooms.booking.domain.entity.ReservationStatus;
import com.meetingrooms.booking.domain.entity.Room;
import com.meetingrooms.booking.domain.repository.ReservationRepository;
import com.meetingrooms.booking.domain.repository.ReservationSpecifications;
import com.meetingrooms.booking.domain.repository.RoomRepository;
import com.meetingrooms.booking.dto.request.CreateReservationRequest;
import com.meetingrooms.booking.dto.response.PageResponse;
import com.meetingrooms.booking.dto.response.ReservationResponse;
import com.meetingrooms.booking.exception.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

@Service
public class ReservationService {

    private static final Duration MIN_RESERVATION_DURATION = Duration.ofMinutes(15);

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        validateTimeRange(request);

        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new RoomNotFoundException(request.roomId()));

        boolean overlaps = reservationRepository.existsOverlapping(
                room.getId(),
                ReservationStatus.CONFIRMED,
                request.startTime(),
                request.endTime()
        );

        if (overlaps) {
            throw new RoomConflictException(room.getId(), request.startTime(), request.endTime());
        }

        Reservation reservation = new Reservation(
                room,
                request.responsible().trim(),
                request.startTime(),
                request.endTime()
        );
        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> listReservations(
            Long roomId,
            LocalDate date,
            boolean includeCancelled,
            Pageable pageable
    ) {
        Specification<Reservation> spec = ReservationSpecifications.withFilters(roomId, date, includeCancelled);
        Page<ReservationResponse> page = reservationRepository.findAll(spec, pageable)
                .map(ReservationResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findWithRoomById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findWithRoomById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationAlreadyCancelledException(reservationId);
        }

        reservation.cancel();
        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.from(saved);
    }

    private void validateTimeRange(CreateReservationRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidTimeRangeException("endTime must be strictly after startTime");
        }
        if (Duration.between(request.startTime(), request.endTime()).compareTo(MIN_RESERVATION_DURATION) < 0) {
            throw new InvalidTimeRangeException("Reservation duration must be at least 15 minutes");
        }
    }
}
