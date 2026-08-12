#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"

event_id=${1:?Usage: verify-reservation-contention.sh EVENT_ID [CAPACITY]}
capacity=${2:-100}
require_command docker

remaining=$(postgres_query "select remaining_capacity from events where id = $event_id")
active=$(postgres_query "select count(*) from reservations where event_id = $event_id and status in ('HELD', 'CONFIRMED')")
[[ "$remaining" == 0 ]] || { echo "Expected remaining capacity 0, found $remaining" >&2; exit 1; }
[[ "$active" == "$capacity" ]] || { echo "Expected $capacity active reservations, found $active" >&2; exit 1; }
echo "Verified event $event_id: active_reservations=$active remaining_capacity=$remaining"
