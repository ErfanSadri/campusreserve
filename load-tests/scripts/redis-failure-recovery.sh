#!/usr/bin/env bash
# Stops only CampusReserve's local Redis container. It never removes volumes.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

require_command curl
require_command docker
require_command jq
redis_stopped=false
restore_redis() {
  if [[ "$redis_stopped" == true ]]; then
    "${COMPOSE[@]}" start redis >/dev/null || true
    wait_for_service_healthy redis || true
  fi
}
trap restore_redis EXIT

wait_for_http_status "$BASE_URL/actuator/health/readiness" 200
event=$(create_event "Redis failure recovery" 2)
event_id=$(jq -r '.id' <<<"$event")
curl -fsS "$BASE_URL/api/events/$event_id" >/dev/null

echo "Stopping CampusReserve Redis only..."
"${COMPOSE[@]}" stop redis
redis_stopped=true

curl -fsS "$BASE_URL/api/events/$event_id" >/dev/null
readiness_status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/actuator/health/readiness" || true)
[[ "$readiness_status" == 200 ]] || { echo "Expected readiness 200 while Redis is down, got $readiness_status" >&2; exit 1; }
[[ $(create_reservation "$event_id" "redis-failure-$event_id" "redis-$event_id@failure.invalid") == 201 ]]
dependencies_status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/actuator/health/dependencies" || true)
[[ "$dependencies_status" == 503 ]] || { echo "Expected dependencies health 503 while Redis is down, got $dependencies_status" >&2; exit 1; }
echo "Redis down: event read and reservation succeeded; readiness=UP; dependencies HTTP status=$dependencies_status"

"${COMPOSE[@]}" start redis >/dev/null
wait_for_service_healthy redis
redis_stopped=false
curl -fsS "$BASE_URL/api/events/$event_id" >/dev/null
echo "Redis recovered successfully."
