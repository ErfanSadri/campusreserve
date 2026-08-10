# CampusReserve

CampusReserve is a university-focused event discovery and registration platform.

Students can discover official campus events while organizations can create and share their own limited-capacity events with reservations and waitlists.

## Current Status

Event and reservation API with PostgreSQL persistence, concurrency-safe and
idempotent reservation creation, Redis event caching, and Kafka reservation
lifecycle events delivered through a transactional outbox. Delivery is
at-least-once with database-backed idempotent consumer processing, bounded
retry, and dead-letter handling.

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
again later. CampusReserve uses at-least-once delivery with database-backed,
idempotent consumer processing; it does not claim Kafka exactly-once semantics.
Consumer failures are retried twice with a 250 ms backoff, then routed to
`campusreserve.reservation.lifecycle.v1.dlt` with the original record headers
and failure metadata for logging visibility.
