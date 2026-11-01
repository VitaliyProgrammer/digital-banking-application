// 📈 Load: a normal busy day. 40 clients with their own accounts look at their money and pay each other.
import { check, sleep } from 'k6';
import { account, exchangeRates, newFundedClient, statement, transfer } from './lib/bank.js';

const CLIENTS = 40;

export const options = {
  // setup() registers and funds 40 clients before the clock starts; it may take a while.
  setupTimeout: '3m',
  scenarios: {
    busy_day: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: CLIENTS },   // ramp-up: clients arrive gradually
        { duration: '3m', target: CLIENTS },   // steady state: this is what we measure
        { duration: '30s', target: 0 },        // ramp-down
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],                  // fewer than 1% of requests fail
    // Only the measured scenario: setup() registers clients, and password hashing (BCrypt) is slow on purpose.
    'http_req_duration{scenario:busy_day}': ['p(95)<500', 'p(99)<1500'],
    'http_req_duration{scenario:busy_day,name:transfer}': ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

/** Runs once, before the load: every VU later picks "its" client from this list. */
export function setup() {
  const clients = [];
  for (let i = 0; i < CLIENTS; i++) {
    clients.push(newFundedClient(100000));
  }
  return { clients };
}

export default function ({ clients }) {
  const me = clients[(__VU - 1) % clients.length];
  const someoneElse = clients[Math.floor(Math.random() * clients.length)];

  check(account(me), { 'account: 200': (r) => r.status === 200 });
  check(statement(me), { 'statement: 200': (r) => r.status === 200 });
  check(exchangeRates(), { 'exchange rates: 200': (r) => r.status === 200 });

  if (someoneElse.iban !== me.iban) {
    check(transfer(me, someoneElse.iban, 50), { 'transfer: 201': (r) => r.status === 201 });
  }

  // "Think time": a real person does not click non-stop. Without it 40 VUs would act like thousands of clients.
  sleep(1 + Math.random());
}
