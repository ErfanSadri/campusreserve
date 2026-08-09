package com.erfansadri.campusreserve.event;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "registration_opens_at", nullable = false)
    private OffsetDateTime registrationOpensAt;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "remaining_capacity", nullable = false)
    private int remainingCapacity;

    @Generated
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false)
    private OffsetDateTime createdAt;

    protected Event() {
    }

    public Event(
            String title,
            String description,
            String location,
            OffsetDateTime startTime,
            OffsetDateTime registrationOpensAt,
            int capacity) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.registrationOpensAt = registrationOpensAt;
        this.capacity = capacity;
        this.remainingCapacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}