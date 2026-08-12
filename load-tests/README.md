# CampusReserve load tests

These k6 scenarios target a running local CampusReserve API. They do not reset
data and each creates its own event. Run them from the repository root.

If k6 is installed locally, use `k6 run`. Otherwise Docker Desktop can run the
pinned image without adding a project dependency:

```bash
docker run --rm -i \
  -v "$PWD/load-tests:/scripts:ro" \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6:0.57.0 run /scripts/reservation-contention.js
```

On Linux, replace `host.docker.internal` with an address reachable from the
container, or install k6 locally and use `BASE_URL=http://localhost:8080`.

Useful commands:

```bash
BASE_URL=http://localhost:8080 k6 run load-tests/reservation-contention.js
BASE_URL=http://localhost:8080 k6 run load-tests/idempotency-contention.js
BASE_URL=http://localhost:8080 k6 run load-tests/event-read-baseline.js
BASE_URL=http://localhost:8080 k6 run load-tests/mixed-workload.js
```

The contention scenario prints its event ID. Verify PostgreSQL state after it
finishes (the script only reads data):

```bash
load-tests/scripts/verify-reservation-contention.sh EVENT_ID 100
```

The idempotency scenario prints its event ID and attendee email. Verify it with:

```bash
load-tests/scripts/verify-idempotency-contention.sh EVENT_ID ATTENDEE_EMAIL
```

Latency statistics are observations, not pass/fail thresholds. Scenario checks
fail only for unexpected responses or correctness violations.
