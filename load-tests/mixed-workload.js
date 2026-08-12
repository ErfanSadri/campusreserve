import { check, sleep } from 'k6';
import { createHold, createOpenEvent, getEvent, reservationPayload } from './lib.js';

const vus = Number(__ENV.MIXED_VUS || 15);
const duration = __ENV.MIXED_DURATION || '30s';

export const options = {
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: { mixed: { executor: 'constant-vus', vus, duration } },
  thresholds: { checks: ['rate==1'], http_req_failed: ['rate==0'] },
};

export function setup() { return createOpenEvent('Mixed workload', 10000); }

export default function (event) {
  if ((__ITER + __VU) % 10 === 0) {
    const email = `mixed-${event.id}-${__VU}-${__ITER}@load.invalid`;
    const response = createHold(
      event.id,
      `mixed-${event.id}-${__VU}-${__ITER}`,
      reservationPayload(`Mixed ${__VU}`, email));
    check(response, { 'mixed reservation succeeds': (result) => result.status === 201 });
  } else {
    const response = getEvent(event.id);
    check(response, { 'mixed event read succeeds': (result) => result.status === 200 });
  }
  sleep(0.05);
}
