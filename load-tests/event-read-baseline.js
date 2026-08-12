import { check, sleep } from 'k6';
import { createOpenEvent, getEvent } from './lib.js';

const vus = Number(__ENV.READ_VUS || 20);
const warmup = __ENV.WARMUP_DURATION || '10s';
const sustained = __ENV.SUSTAINED_DURATION || '30s';
const cooldown = __ENV.COOLDOWN_DURATION || '5s';

export const options = {
  summaryTrendStats: ['min', 'med', 'avg', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    warmup: { executor: 'constant-vus', vus, duration: warmup, exec: 'readEvent' },
    sustained: {
      executor: 'constant-vus', vus, duration: sustained, startTime: warmup, exec: 'readEvent',
    },
    cooldown: {
      executor: 'constant-vus', vus: Math.max(1, Math.floor(vus / 2)), duration: cooldown,
      startTime: `${Number(warmup.replace('s', '')) + Number(sustained.replace('s', ''))}s`, exec: 'readEvent',
    },
  },
  thresholds: { checks: ['rate==1'], http_req_failed: ['rate==0'] },
};

export function setup() { return createOpenEvent('Event read baseline', 20); }

export function readEvent(event) {
  const response = getEvent(event.id);
  check(response, { 'event read succeeds': (result) => result.status === 200 });
  sleep(0.05);
}
