# CampusReserve

CampusReserve is a university-focused event discovery and registration platform.

Students can discover official campus events while organizations can create and share their own limited-capacity events with reservations and waitlists.

## Current Status

Project foundation in progress.

## Planned Stack

- Java
- Spring Boot
- PostgreSQL
- Redis
- Apache Kafka
- Docker
- AWS
- Terraform

## Applications

- `reservation-api` — HTTP API for events and reservations

Additional services will be introduced only when required by the application architecture.

## Development

Run tests:

```bash
cd apps/reservation-api
./mvnw test
```

## Kafka lifecycle events

Reservation hold creation, confirmation, and cancellation write versioned
events to a PostgreSQL transactional outbox. A background publisher delivers
them to `campusreserve.reservation.lifecycle.v1` after the database transaction
commits. Kafka may receive an event immediately before the process fails to
persist `published_at`, so the same stable outbox event ID can be delivered
again later. CRV-010 will make consumers idempotent and add retry/DLT behavior.
