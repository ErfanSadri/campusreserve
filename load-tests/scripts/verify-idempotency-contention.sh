#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"

event_id=${1:?Usage: verify-idempotency-contention.sh EVENT_ID ATTENDEE_EMAIL}
email=${2:?Usage: verify-idempotency-contention.sh EVENT_ID ATTENDEE_EMAIL}
require_command docker

remaining=$(postgres_query "select remaining_capacity from events where id = $event_id")
active=$(postgres_query "select count(*) from reservations where event_id = $event_id and lower(attendee_email) = lower('$email') and status in ('HELD', 'CONFIRMED')")
[[ "$remaining" == 1 ]] || { echo "Expected one seat consumed, remaining capacity is $remaining" >&2; exit 1; }
[[ "$active" == 1 ]] || { echo "Expected one active reservation for $email, found $active" >&2; exit 1; }
echo "Verified idempotency event $event_id: active_reservations=$active remaining_capacity=$remaining"
