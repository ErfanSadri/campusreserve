# CRV-013 performance and failure testing

These are local development measurements, not production-capacity claims. They
are intended to exercise CampusReserve's existing locking, idempotency, cache,
outbox, and dependency-boundary behavior.

## Environment

- Date: 2026-08-12
- macOS 26.5.2; Java 26.0.1 ran this local application session
- PostgreSQL 18, Redis 7.4.2, and Kafka 4.0.0 ran in the repository's Docker
  Compose environment
- k6 0.57.0 ran from the pinned `grafana/k6:0.57.0` Docker image
- API target: locally started `http://localhost:8080`; Docker k6 reached it
  through `http://host.docker.internal:8080`

The project targets Java 21; the local JDK version is recorded only to make
these measurements reproducible.

## Reproducing the load scenarios

Start the Compose dependencies and the API, then run the commands in
[`load-tests/README.md`](../load-tests/README.md). The four scenarios each
create their own event and accept `BASE_URL` (defaulting to
`http://localhost:8080`). The contention and idempotency scenarios print the
values required by their PostgreSQL verification helpers.

## Successful local benchmark runs

| Scenario | Configuration | Requests / rate | HTTP timing | Result |
| --- | --- | ---: | --- | --- |
| Reservation contention | one event, capacity 100; 500 concurrent one-shot VUs | 502 requests; 125.24 req/s | median 2.17 s; p90 3.29 s; p95 3.45 s; p99 3.54 s; max 3.56 s | 100 holds, 400 expected capacity rejections, 0 unexpected failures; PostgreSQL verification found 100 active reservations and remaining capacity 0. |
| Idempotency contention | one event; 100 concurrent identical requests with one key | 103 requests; 238.71 req/s | median 177.29 ms; p90 303.67 ms; p95 321.85 ms; p99 336.32 ms; max 339.85 ms | all 100 requests returned the existing hold; one different-payload replay returned 409; PostgreSQL verification found one active reservation and remaining capacity 1. |
| Event reads | 20 VUs; 5 s warm-up, 15 s sustained, 10-VU 3 s cool-down | 7,627 requests; 331.30 req/s | median 5.15 ms; p90 7.12 ms; p95 8.35 ms; p99 15.37 ms; max 35.19 ms | 0 HTTP failures and 7,627/7,627 checks passed. This repeatedly reads one event through the existing cache-aside path. |
| Mixed workload | 15 VUs for 15 s; approximately nine reads per reservation attempt | 3,865 requests; 256.29 req/s | median 4.53 ms; p90 16.12 ms; p95 20.85 ms; p99 34.22 ms; max 106.97 ms | 0 HTTP failures and 3,865/3,865 checks passed. |

The contention scenario treats the existing legitimate 409 capacity response as
an expected application outcome, rather than as a k6 transport failure. Its
correctness checks are intentionally separate from latency observations, so a
slower local machine does not turn a valid no-overbooking result into a false
failure.

Selected cumulative Actuator evidence from the same local application session
showed `campusreserve_outbox_events_published_total=690`,
`campusreserve_kafka_lifecycle_processed_total=690`, and
`campusreserve_kafka_lifecycle_dlt_arrivals_total=0`. These are process-wide
counters, not per-scenario totals.

## Local dependency failure and recovery runs

The scripts under `load-tests/scripts/` operate only on the repository's
Compose services. They stop and restart a single named service, use bounded
polling, restore it via a shell trap, and never remove containers or volumes.

- `redis-failure-recovery.sh`: after Redis stopped, `GET /api/events/{id}` and
  a reservation creation still succeeded through PostgreSQL; readiness stayed
  200 and the dependencies health group returned 503. Redis restarted healthy
  and normal event retrieval resumed.
- `kafka-outbox-recovery.sh`: with Kafka stopped after the API was running, a
  reservation committed as reservation 3998 and exactly one unpublished
  outbox row existed. Readiness remained 200. After Kafka restarted healthy,
  that row had `published_at` set and the audit consumer created its processing
  record.
- `postgres-failure-recovery.sh`: stopping PostgreSQL produced readiness 503;
  the database-backed create-event request did not succeed (it timed out in
  this local run). After PostgreSQL restarted healthy, readiness returned 200
  and a normal create-event request succeeded (event 523).

Run these scripts only against the local CampusReserve Compose environment:

```bash
load-tests/scripts/redis-failure-recovery.sh
load-tests/scripts/kafka-outbox-recovery.sh
load-tests/scripts/postgres-failure-recovery.sh
```

Kafka downtime is deliberately non-fatal to reservation transactions because
the transactional outbox retains the pending lifecycle event. PostgreSQL is
authoritative, so its failure makes readiness unavailable and prevents normal
database-backed writes. Redis remains a non-authoritative event-read cache and
its failure falls back to PostgreSQL.
