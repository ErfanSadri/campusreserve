import { Counter, Trend } from 'k6/metrics';
import { check } from 'k6';
import { createHold, createOpenEvent, getEvent, reservationPayload } from './lib.js';

const capacity = Number(__ENV.EVENT_CAPACITY || 100);
const attempts = Number(__ENV.ATTEMPTS || 500);
const successes = new Counter('reservation_contention_successes');
const capacityRejections = new Counter('reservation_contention_capacity_rejections');
const unexpectedFailures = new Counter('reservation_contention_unexpected_failures');
const reservationLatency = new Trend('reservation_contention_latency', true);

export const options = {
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    contention: {
      executor: 'per-vu-iterations',
      vus: attempts,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {
    checks: ['rate==1'],
    reservation_contention_unexpected_failures: ['count==0'],
  },
};

export function setup() {
  return createOpenEvent('Reservation contention', capacity);
}

export default function (event) {
  const email = `contention-${event.id}-${__VU}@load.invalid`;
  const response = createHold(
    event.id,
    `contention-${event.id}-${__VU}`,
    reservationPayload(`Contention ${__VU}`, email));
  reservationLatency.add(response.timings.duration);

  if (response.status === 201) {
    successes.add(1);
  } else if (response.status === 409 && response.body.includes('no remaining capacity')) {
    capacityRejections.add(1);
  } else {
    unexpectedFailures.add(1);
  }
  check(response, {
    'reservation is accepted or legitimately capacity-rejected': (result) =>
      result.status === 201 || (result.status === 409 && result.body.includes('no remaining capacity')),
  });
}

export function teardown(event) {
  const response = getEvent(event.id);
  check(response, {
    'contention event is readable': (result) => result.status === 200,
    'remaining capacity is zero': (result) => result.status === 200 && result.json().remainingCapacity === 0,
  });
  console.log(`CONTENTION_EVENT_ID=${event.id} expected_capacity=${capacity} expected_attempts=${attempts}`);
}
