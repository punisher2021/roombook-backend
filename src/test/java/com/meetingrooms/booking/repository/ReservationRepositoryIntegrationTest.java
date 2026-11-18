package com.meetingrooms.booking.repository;

import com.meetingrooms.booking.domain.entity.Reservation;
import com.meetingrooms.booking.domain.entity.ReservationStatus;
import com.meetingrooms.booking.domain.entity.Room;
import com.meetingrooms.booking.domain.repository.ReservationRepository;
import com.meetingrooms.booking.domain.repository.ReservationSpecifications;
import com.meetingrooms.booking.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRepositoryIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Room roomA;
    private Room roomB;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        roomRepository.deleteAll();

        roomA = roomRepository.save(new Room("Sala Mercurio", 8, "Planta 1"));
        roomB = roomRepository.save(new Room("Sala Venus", 12, "Planta 2"));
    }

    @Test
    void shouldDetectOverlapOnlyForConfirmedAndIntersectingIntervals() {
        LocalDate day = LocalDate.now().plusDays(2);
        LocalDateTime tenAm = day.atTime(10, 0);
        LocalDateTime elevenAm = day.atTime(11, 0);

        reservationRepository.save(new Reservation(roomA, "Elena Díaz", tenAm, elevenAm));

        // Partial overlap -> true
        assertThat(reservationRepository.existsOverlapping(
                roomA.getId(), ReservationStatus.CONFIRMED, day.atTime(10, 30), day.atTime(11, 30)))
                .isTrue();

        // Back-to-back boundary [11:00, 12:00) -> false
        assertThat(reservationRepository.existsOverlapping(
                roomA.getId(), ReservationStatus.CONFIRMED, elevenAm, day.atTime(12, 0)))
                .isFalse();

        // Different room -> false
        assertThat(reservationRepository.existsOverlapping(
                roomB.getId(), ReservationStatus.CONFIRMED, tenAm, elevenAm))
                .isFalse();
    }

    @Test
    void shouldFilterByRoomAndDateWithPagination() {
        LocalDate targetDay = LocalDate.now().plusDays(3);
        LocalDate otherDay = targetDay.plusDays(1);

        reservationRepository.save(new Reservation(roomA, "User 1", targetDay.atTime(9, 0), targetDay.atTime(10, 0)));
        reservationRepository.save(new Reservation(roomA, "User 2", targetDay.atTime(11, 0), targetDay.atTime(12, 0)));
        reservationRepository.save(new Reservation(roomA, "User 3", otherDay.atTime(9, 0), otherDay.atTime(10, 0)));
        reservationRepository.save(new Reservation(roomB, "User 4", targetDay.atTime(9, 0), targetDay.atTime(10, 0)));

        Page<Reservation> page = reservationRepository.findAll(
                ReservationSpecifications.withFilters(roomA.getId(), targetDay, false),
                PageRequest.of(0, 1)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }
}
