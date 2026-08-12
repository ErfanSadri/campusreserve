package com.erfansadri.campusreserve.waitlist;

import java.time.OffsetDateTime;

import com.erfansadri.campusreserve.event.Event;
import com.erfansadri.campusreserve.reservation.Reservation;

import org.hibernate.annotations.Generated;

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

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

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
    private WaitlistStatus status;

    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "promoted_at")
    private OffsetDateTime promotedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_reservation_id")
    private Reservation promotedReservation;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(Event event, String attendeeName, String attendeeEmail) {
        this.event = event;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.status = WaitlistStatus.WAITING;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getAttendeeName() { return attendeeName; }
    public String getAttendeeEmail() { return attendeeEmail; }
    public WaitlistStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getPromotedAt() { return promotedAt; }
    public Reservation getPromotedReservation() { return promotedReservation; }

    public void promote(Reservation reservation, OffsetDateTime promotedAt) {
        if (status != WaitlistStatus.WAITING) {
            throw new IllegalStateException("Only waiting entries can be promoted.");
        }
        status = WaitlistStatus.PROMOTED;
        this.promotedReservation = reservation;
        this.promotedAt = promotedAt;
    }
}
