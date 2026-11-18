package com.meetingrooms.booking.service;

import com.meetingrooms.booking.domain.entity.Reservation;
import com.meetingrooms.booking.domain.entity.ReservationStatus;
import com.meetingrooms.booking.domain.entity.Room;
import com.meetingrooms.booking.domain.repository.ReservationRepository;
import com.meetingrooms.booking.domain.repository.RoomRepository;
import com.meetingrooms.booking.dto.request.CreateReservationRequest;
import com.meetingrooms.booking.dto.response.ReservationResponse;
import com.meetingrooms.booking.exception.InvalidTimeRangeException;
import com.meetingrooms.booking.exception.RoomConflictException;
import com.meetingrooms.booking.exception.RoomNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room("Sala Atlas", 10, "Planta 3");
        ReflectionTestUtils.setField(room, "id", 1L);
    }

    @Test
    void shouldCreateReservationWhenSlotIsAvailable() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        CreateReservationRequest request = new CreateReservationRequest(1L, "Laura Gómez", start, end);

        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlapping(1L, ReservationStatus.CONFIRMED, start, end)).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        ReservationResponse response = reservationService.createReservation(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.responsible()).isEqualTo("Laura Gómez");
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void shouldThrowRoomConflictExceptionWhenOverlappingReservationExists() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        CreateReservationRequest request = new CreateReservationRequest(1L, "Carlos Ruiz", start, end);

        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlapping(1L, ReservationStatus.CONFIRMED, start, end)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(RoomConflictException.class)
                .hasMessageContaining("already booked");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidTimeRangeExceptionWhenEndIsNotAfterStart() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        CreateReservationRequest request = new CreateReservationRequest(1L, "Ana Torres", start, start);

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(InvalidTimeRangeException.class)
                .hasMessageContaining("strictly after");

        verify(roomRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldThrowRoomNotFoundExceptionWhenRoomDoesNotExist() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = start.plusHours(1);
        CreateReservationRequest request = new CreateReservationRequest(99L, "Ana Torres", start, end);

        when(roomRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(RoomNotFoundException.class);
    }
}
