#!/usr/bin/env bash
# Stops only CampusReserve's local Kafka container. It never removes volumes.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

require_command curl
require_command docker
kafka_stopped=false
restore_kafka() {
  if [[ "$kafka_stopped" == true ]]; then
    "${COMPOSE[@]}" start kafka >/dev/null || true
    wait_for_service_healthy kafka || true
  fi
}
trap restore_kafka EXIT

wait_for_http_status "$BASE_URL/actuator/health/readiness" 200
event=$(create_event "Kafka outbox recovery" 2)
event_id=$(jq -r '.id' <<<"$event")

echo "Stopping CampusReserve Kafka only..."
"${COMPOSE[@]}" stop kafka
kafka_stopped=true

[[ $(create_reservation "$event_id" "kafka-failure-$event_id" "kafka-$event_id@failure.invalid") == 201 ]]
reservation_id=$(jq -r '.id' /tmp/campusreserve-reservation-response.json)
pending=$(postgres_query "select count(*) from outbox_events where aggregate_id = $reservation_id and published_at is null")
[[ "$pending" == 1 ]] || { echo "Expected one unpublished outbox event, found $pending" >&2; exit 1; }
[[ $(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL/actuator/health/readiness") == 200 ]]
echo "Kafka down: reservation $reservation_id committed and one outbox event is pending; readiness=UP."

"${COMPOSE[@]}" start kafka >/dev/null
wait_for_service_healthy kafka
kafka_stopped=false

published=false
for ((i = 1; i <= 60; i++)); do
  published=$(postgres_query "select case when published_at is not null then 'true' else 'false' end from outbox_events where aggregate_id = $reservation_id order by created_at desc limit 1")
  [[ "$published" == true ]] && break
  sleep 1
done
[[ "$published" == true ]] || { echo "Timed out waiting for the outbox event to publish" >&2; exit 1; }

processed=false
for ((i = 1; i <= 60; i++)); do
  processed=$(postgres_query "select exists (select 1 from processed_consumer_events p join outbox_events o on o.id = p.outbox_event_id where o.aggregate_id = $reservation_id and p.consumer_name = 'reservation-lifecycle-audit-v1')")
  [[ "$processed" == true ]] && break
  sleep 1
done
[[ "$processed" == true ]] || { echo "Timed out waiting for audit consumer processing" >&2; exit 1; }
echo "Kafka recovered: pending outbox event published and processed exactly through the existing consumer path."
