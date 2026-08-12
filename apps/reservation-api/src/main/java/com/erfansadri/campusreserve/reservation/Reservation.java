package com.erfansadri.campusreserve.reservation;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;

import com.erfansadri.campusreserve.event.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "attendee_name", nullable = false, length = 150)
    private String attendeeName;

    @Column(name = "attendee_email", nullable = false, length = 320)
    private String attendeeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "held_until")
    private OffsetDateTime heldUntil;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "request_fingerprint",
            length = 64,
            columnDefinition = "CHAR(64)")
    private String requestFingerprint;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false)
    private OffsetDateTime createdAt;

    @Generated
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Reservation() {
    }

    public Reservation(
            Event event,
            String attendeeName,
            String attendeeEmail,
            OffsetDateTime heldUntil) {
        this(
                event,
                attendeeName,
                attendeeEmail,
                heldUntil,
                null,
                null);
    }

    public Reservation(
            Event event,
            String attendeeName,
            String attendeeEmail,
            OffsetDateTime heldUntil,
            String idempotencyKey,
            String requestFingerprint) {
        this.event = event;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.status = ReservationStatus.HELD;
        this.heldUntil = heldUntil;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getHeldUntil() {
        return heldUntil;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void confirm() {
        if (status != ReservationStatus.HELD) {
            throw new InvalidReservationStateException(
                    "Only held reservations can be confirmed.");
        }

        status = ReservationStatus.CONFIRMED;
        heldUntil = null;
        updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (status != ReservationStatus.HELD
                && status != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException(
                    "Only active reservations can be cancelled.");
        }

        status = ReservationStatus.CANCELLED;
        heldUntil = null;
        updatedAt = OffsetDateTime.now();
    }

    public void expire() {
        if (status != ReservationStatus.HELD) {
            throw new InvalidReservationStateException(
                    "Only held reservations can expire.");
        }

        status = ReservationStatus.EXPIRED;
        heldUntil = null;
        updatedAt = OffsetDateTime.now();
    }
}
