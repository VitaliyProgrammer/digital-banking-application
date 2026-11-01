// What a client of the bank does, as plain functions over the HTTP API. Every scenario builds on these.
import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const PASSWORD = 'load-test-password-1';

/** RFC 4122 version 4: unique enough for test e-mails and Idempotency-Key headers. */
export function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/** Registers a new client, logs in, opens a UAH account and tops it up. Returns what later requests need. */
export function newFundedClient(amount) {
  const email = `load-${uuid()}@bank.test`;

  const registered = http.post(`${BASE_URL}/authentication/register`, JSON.stringify({
    email, password: PASSWORD, firstName: 'Load', lastName: 'Test',
  }), { headers: JSON_HEADERS, tags: { name: 'register' } });
  must(registered, 201, 'register');

  const login = http.post(`${BASE_URL}/authentication/login`, JSON.stringify({ email, password: PASSWORD }),
    { headers: JSON_HEADERS, tags: { name: 'login' } });
  must(login, 200, 'login');
  const token = login.json('accessToken');

  const opened = http.post(`${BASE_URL}/accounts`, JSON.stringify({ currency: 'UAH' }),
    { headers: auth(token), tags: { name: 'open account' } });
  must(opened, 201, 'open account');

  const client = { token, accountId: opened.json('id'), iban: opened.json('number') };
  if (amount) {
    must(topUp(client, amount), 201, 'top up');
  }
  return client;
}

export function topUp(client, amount) {
  return http.post(`${BASE_URL}/payments/top-up`, JSON.stringify({ accountId: client.accountId, amount }),
    { headers: { ...auth(client.token), 'Idempotency-Key': uuid() }, tags: { name: 'top up' } });
}

/** The key is a parameter: a scenario may send the same key twice to test idempotency. */
export function transfer(from, toIban, amount, idempotencyKey = uuid()) {
  return http.post(`${BASE_URL}/payments/transfers`, JSON.stringify({
    fromAccountId: from.accountId, toAccountNumber: toIban, amount, description: 'k6',
  }), { headers: { ...auth(from.token), 'Idempotency-Key': idempotencyKey }, tags: { name: 'transfer' } });
}

export function accounts(client) {
  return http.get(`${BASE_URL}/accounts`, { headers: auth(client.token), tags: { name: 'accounts' } });
}

export function account(client) {
  return http.get(`${BASE_URL}/accounts/${client.accountId}`, { headers: auth(client.token), tags: { name: 'account' } });
}

export function statement(client) {
  return http.get(`${BASE_URL}/accounts/${client.accountId}/transactions?size=20`,
    { headers: auth(client.token), tags: { name: 'statement' } });
}

export function exchangeRates() {
  return http.get(`${BASE_URL}/exchange-rates`, { tags: { name: 'exchange rates' } });
}

function auth(token) {
  return { ...JSON_HEADERS, Authorization: `Bearer ${token}` };
}

/** In setup a failure means the test cannot even start: stop with a clear message instead of measuring nonsense. */
function must(response, status, step) {
  if (!check(response, { [`${step}: ${status}`]: (r) => r.status === status })) {
    fail(`${step} answered ${response.status}: ${response.body}`);
  }
}
