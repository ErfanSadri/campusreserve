package com.erfansadri.campusreserve.reservation;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByEvent_IdAndAttendeeEmailIgnoreCaseAndStatusIn(
            Long eventId,
            String attendeeEmail,
            Collection<ReservationStatus> statuses);

    Optional<Reservation> findByEvent_IdAndIdempotencyKey(
        Long eventId,
        String idempotencyKey);

    List<Reservation> findByEvent_Id(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.status = :status AND r.heldUntil <= :now
            ORDER BY r.heldUntil ASC, r.id ASC
            """)
    List<Reservation> findOverdueHoldsForUpdateSkipLocked(
            @Param("status") ReservationStatus status,
            @Param("now") OffsetDateTime now,
            Pageable pageable);
}
