// 💨 Smoke: one client walks the main path for 30 seconds. If this fails, a bigger test would only measure a bug.
import { check, sleep } from 'k6';
import { accounts, newFundedClient, statement, transfer } from './lib/bank.js';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    checks: ['rate==1'],            // every single check passes
    http_req_failed: ['rate==0'],   // no request fails at all
  },
};

export default function () {
  const sender = newFundedClient(1000);
  const recipient = newFundedClient(0);

  check(transfer(sender, recipient.iban, 100), { 'transfer: 201': (r) => r.status === 201 });
  check(accounts(sender), { 'accounts: 200': (r) => r.status === 200 });
  check(statement(sender), {
    'statement: top-up and transfer are there': (r) => r.status === 200 && r.json('content').length === 2,
  });

  sleep(1);
}
