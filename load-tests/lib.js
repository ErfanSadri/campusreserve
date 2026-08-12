import http from 'k6/http';
import { check } from 'k6';

export const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export function createOpenEvent(titlePrefix, capacity) {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
  const now = new Date();
  const registrationOpensAt = new Date(now.getTime() - 60 * 1000).toISOString();
  const startTime = new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString();
  const response = http.post(`${baseUrl}/api/events`, JSON.stringify({
    title: `${titlePrefix} ${suffix}`,
    description: 'Local k6 scenario event',
    location: 'Load Test Lab',
    startTime,
    registrationOpensAt,
    capacity,
  }), {
    headers: { 'Content-Type': 'application/json' },
    responseCallback: http.expectedStatuses(201),
  });
  check(response, { 'event creation succeeded': (result) => result.status === 201 });
  if (response.status !== 201) {
    throw new Error(`Could not create test event: HTTP ${response.status} ${response.body}`);
  }
  return response.json();
}

export function reservationPayload(name, email) {
  return JSON.stringify({ attendeeName: name, attendeeEmail: email });
}

export function createHold(eventId, idempotencyKey, payload) {
  return http.post(`${baseUrl}/api/events/${eventId}/reservations`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    responseCallback: http.expectedStatuses(201, 409),
  });
}

export function getEvent(eventId) {
  return http.get(`${baseUrl}/api/events/${eventId}`);
}
