#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
BASE_URL=${BASE_URL:-http://localhost:8080}
COMPOSE=(docker compose --project-directory "$ROOT_DIR" -f "$ROOT_DIR/compose.yaml")

require_command() {
  command -v "$1" >/dev/null 2>&1 || { echo "Required command not found: $1" >&2; exit 1; }
}

wait_for_http_status() {
  local url=$1 expected=$2 attempts=${3:-30} code
  for ((i = 1; i <= attempts; i++)); do
    code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 5 "$url" || true)
    if [[ "$code" == "$expected" ]]; then return 0; fi
    sleep 1
  done
  echo "Timed out waiting for $url to return $expected" >&2
  return 1
}

wait_for_service_healthy() {
  local service=$1 container status
  for ((i = 1; i <= 60; i++)); do
    container=$("${COMPOSE[@]}" ps -q "$service")
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)
    if [[ "$status" == healthy ]]; then return 0; fi
    sleep 1
  done
  echo "Timed out waiting for $service to become healthy" >&2
  return 1
}

create_event() {
  local title=$1 capacity=$2
  curl -fsS -X POST "$BASE_URL/api/events" \
    -H 'Content-Type: application/json' \
    -d "{\"title\":\"$title $(date +%s)\",\"description\":\"Local failure test\",\"location\":\"Load Test Lab\",\"startTime\":\"2035-01-01T12:00:00Z\",\"registrationOpensAt\":\"2020-01-01T12:00:00Z\",\"capacity\":$capacity}"
}

create_reservation() {
  local event_id=$1 key=$2 email=$3
  curl -sS -o /tmp/campusreserve-reservation-response.json -w '%{http_code}' \
    -X POST "$BASE_URL/api/events/$event_id/reservations" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "{\"attendeeName\":\"Failure Test\",\"attendeeEmail\":\"$email\"}"
}

postgres_query() {
  "${COMPOSE[@]}" exec -T postgres psql -U campusreserve -d campusreserve -Atqc "$1"
}
