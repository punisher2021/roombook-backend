package com.meetingrooms.booking.domain.repository;

import com.meetingrooms.booking.domain.entity.Reservation;
import com.meetingrooms.booking.domain.entity.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<Reservation> withFilters(
            Long roomId,
            LocalDate date,
            boolean includeCancelled
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (roomId != null) {
                predicates.add(cb.equal(root.get("room").get("id"), roomId));
            }

            if (date != null) {
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
                predicates.add(cb.lessThan(root.get("startTime"), dayEnd));
                predicates.add(cb.greaterThan(root.get("endTime"), dayStart));
            }

            if (!includeCancelled) {
                predicates.add(cb.equal(root.get("status"), ReservationStatus.CONFIRMED));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
