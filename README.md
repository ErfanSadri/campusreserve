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

## CI and container image

GitHub Actions runs for pull requests targeting `main` and pushes to `main`.
It uses Java 21, validates the Compose and local harness configuration, starts
the repository's PostgreSQL, Redis, and Kafka services, runs the full Maven
verification, builds the API image, and checks its database-backed readiness.
On a successful trusted push to `main`, it publishes
`ghcr.io/<repository-owner>/campusreserve-reservation-api` with immutable
commit-SHA and `main` tags. Pull requests never publish images.

Build the same production-oriented image locally from the repository root:

```bash
docker build -t campusreserve/reservation-api:local apps/reservation-api
```

Runtime PostgreSQL, Redis, Kafka, and OTLP settings remain environment
variables; no credentials are included in the image. AWS deployment is
deferred to CRV-015.

## Local load and failure testing

Reproducible k6 scenarios and local dependency-failure harnesses are in
[`load-tests/`](load-tests/README.md). Measured local-development results and
their environment are recorded in
[`docs/06-performance-and-failure-testing.md`](docs/06-performance-and-failure-testing.md).

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

## Hold expiration and waitlist promotion

Overdue held reservations are processed in bounded batches using PostgreSQL
row locks with `SKIP LOCKED`. Expiration, and cancellation of an active
reservation, release capacity, evict the event cache, and record their
respective lifecycle outbox events in the same transaction. Each release then
promotes the oldest eligible waiting attendee to one new ten-minute hold, with
the usual `reservation.hold.created` outbox event. Promotion is serialized by
the existing event row lock; Redis is not used for coordination.

Students can join a waitlist only for a full event whose registration is open
and whose start time has not passed. An attendee cannot have both an active
reservation and an active waitlist entry for the same event. Waitlist entries
are intentionally one-way in this milestone: there is no cancellation or
position endpoint.

## Observability

Spring Boot Actuator exposes `/actuator/health` and `/actuator/prometheus`.
`/actuator/health/liveness` reports process liveness, while readiness is
database-backed because PostgreSQL is the source of truth. The
`/actuator/health/dependencies` group reports available dependency health
contributors such as the database and Redis; their failure does not make the
database-backed reservation API unready. Kafka has no custom Actuator health
contributor in this application. Its operational state is instead surfaced by
outbox and messaging metrics, including failed publications and DLT arrivals.

Prometheus metrics use the `campusreserve.*` namespace for successful
reservation and waitlist transitions, outbox publishing, Kafka processing,
duplicates, DLT arrivals, and hold/outbox/expiration timing. HTTP requests
accept a bounded `X-Correlation-ID` or generate one, return it in the response,
and include it in logs. Tracing uses Spring Boot's OpenTelemetry support with
OTLP export disabled locally by default; set `CAMPUSRESERVE_OTLP_ENABLED=true`
and `CAMPUSRESERVE_OTLP_ENDPOINT` to enable a standard OTLP collector.
