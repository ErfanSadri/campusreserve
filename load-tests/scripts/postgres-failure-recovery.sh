#!/usr/bin/env bash
# Stops only CampusReserve's local PostgreSQL container. It never removes volumes.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

require_command curl
require_command docker
postgres_stopped=false
restore_postgres() {
  if [[ "$postgres_stopped" == true ]]; then
    "${COMPOSE[@]}" start postgres >/dev/null || true
    wait_for_service_healthy postgres || true
  fi
}
trap restore_postgres EXIT

wait_for_http_status "$BASE_URL/actuator/health/readiness" 200
echo "Stopping CampusReserve PostgreSQL only..."
"${COMPOSE[@]}" stop postgres
postgres_stopped=true

readiness_status=""
for ((i = 1; i <= 45; i++)); do
  readiness_status=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 3 "$BASE_URL/actuator/health/readiness" || true)
  [[ "$readiness_status" == 503 ]] && break
  sleep 1
done
[[ "$readiness_status" == 503 ]] || { echo "Expected readiness 503 while PostgreSQL is down, got $readiness_status" >&2; exit 1; }
write_status=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 10 -X POST "$BASE_URL/api/events" -H 'Content-Type: application/json' -d '{"title":"Postgres failure","location":"Load Test Lab","startTime":"2035-01-01T12:00:00Z","registrationOpensAt":"2020-01-01T12:00:00Z","capacity":1}' || true)
[[ "$write_status" != 201 ]] || { echo "Database-backed write unexpectedly succeeded" >&2; exit 1; }
echo "PostgreSQL down: readiness=$readiness_status and write status=$write_status."

"${COMPOSE[@]}" start postgres >/dev/null
wait_for_service_healthy postgres
postgres_stopped=false
wait_for_http_status "$BASE_URL/actuator/health/readiness" 200 60
event=$(create_event "Postgres recovery" 1)
event_id=$(jq -r '.id' <<<"$event")
[[ -n "$event_id" && "$event_id" != null ]]
echo "PostgreSQL recovered and a normal database-backed write succeeded (event $event_id)."
