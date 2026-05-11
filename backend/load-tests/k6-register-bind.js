import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
  scenarios: {
    steady_half_day: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 1),
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: Number(__ENV.VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 80),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    duplicate_or_conflict: ['count<10'],
  },
};

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const phoneBase = Number(__ENV.PHONE_BASE || 13900000000);
const conflictCounter = new Counter('duplicate_or_conflict');

function uniquePhone() {
  const offset = ((__VU * 1000000) + __ITER) % 99999999;
  return String(phoneBase + offset);
}

export default function () {
  const phone = uniquePhone();
  const password = '123456';
  const studentId = `S${phone.slice(-6)}`;

  const register = http.post(
    `${baseUrl}/api/auth/register`,
    JSON.stringify({ phone, password, confirmPassword: password }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(register, {
    'register ok or duplicate': (res) => res.status === 200 || res.status === 409,
    'register no server error': (res) => res.status < 500,
  });
  if (register.status === 409) {
    conflictCounter.add(1);
    return;
  }
  if (register.status !== 200) {
    sleep(1);
    return;
  }

  const user = register.json();
  const bind = http.post(
    `${baseUrl}/api/tickets/new-user-bind`,
    JSON.stringify({
      ticketType: 1,
      userId: user.id,
      studentId,
      phone,
    }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(bind, {
    'bind ok or rate limited': (res) => res.status === 200 || res.status === 429,
    'bind no server error': (res) => res.status < 500,
  });

  sleep(1);
}
