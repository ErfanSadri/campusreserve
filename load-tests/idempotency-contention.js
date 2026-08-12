import { Counter } from 'k6/metrics';
import { check } from 'k6';
import { createHold, createOpenEvent, getEvent, reservationPayload } from './lib.js';

const concurrentReplays = Number(__ENV.REPLAYS || 100);
const replaySuccesses = new Counter('idempotency_replay_successes');
const conflictResponses = new Counter('idempotency_conflict_responses');
const unexpectedFailures = new Counter('idempotency_unexpected_failures');

export const options = {
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    identical_replays: {
      executor: 'per-vu-iterations',
      vus: concurrentReplays,
      iterations: 1,
      maxDuration: '1m',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    idempotency_unexpected_failures: ['count==0'],
  },
};

export function setup() {
  const event = createOpenEvent('Idempotency contention', 2);
  return {
    event,
    email: `idempotency-${event.id}@load.invalid`,
    key: `idempotency-${event.id}`,
  };
}

export default function (data) {
  const response = createHold(
    data.event.id,
    data.key,
    reservationPayload('Idempotent Attendee', data.email));
  if (response.status === 201) {
    replaySuccesses.add(1);
  } else {
    unexpectedFailures.add(1);
  }
  check(response, { 'identical replay returns the existing hold': (result) => result.status === 201 });
}

export function teardown(data) {
  const differentPayload = reservationPayload('Different Attendee', `different-${data.event.id}@load.invalid`);
  const conflict = createHold(data.event.id, data.key, differentPayload);
  if (conflict.status === 409) {
    conflictResponses.add(1);
  }
  const event = getEvent(data.event.id);
  check(conflict, { 'different request fingerprint conflicts': (result) => result.status === 409 });
  check(event, {
    'idempotency event remains readable': (result) => result.status === 200,
    'identical concurrent replays consume one seat': (result) =>
      result.status === 200 && result.json().remainingCapacity === 1,
  });
  console.log(`IDEMPOTENCY_EVENT_ID=${data.event.id} attendee_email=${data.email}`);
}
