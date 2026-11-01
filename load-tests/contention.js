// 🔥 Contention: the hardest place of any bank. 30 clients at once take money from ONE account, every request is
// sent twice with the same Idempotency-Key, and the money keeps going until it runs out. At the end, teardown checks
// that not a single kopiyka was created or lost.
import http from 'k6/http';
import { check } from 'k6';
import { account, newFundedClient, transfer, uuid } from './lib/bank.js';

// Enough for ~1 000 transfers: less than 30 VUs push through in a minute, so the money really runs out.
const INITIAL = 15000;
const AMOUNT = 10;
// Transfer fee: 0.5%, but at least 5.00 UAH; for 10 UAH that is the minimum.
const FEE = 5;

// 422 "insufficient funds" is the correct answer once the money is gone: not a failure of the system.
// 200 is for the logins in setup and the balance checks in teardown.
http.setResponseCallback(http.expectedStatuses(200, 201, 422));

export const options = {
  scenarios: {
    one_hot_account: {
      executor: 'constant-vus',
      vus: 30,
      duration: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],                // no 5xx, no timeouts: only 201 and 422 are expected
    'checks{kind:ledger}': ['rate==1'],            // the money check at the end is all-or-nothing
    'checks{kind:idempotency}': ['rate==1'],       // a replay never moves money a second time
    // One account takes its transfers one at a time (row lock) BY DESIGN, so with 30 clients on it this measures
    // the length of that queue, not the speed of the bank. The bar only catches it getting much worse.
    'http_req_duration{name:transfer}': ['p(95)<2000'],
  },
};

export function setup() {
  return { sender: newFundedClient(INITIAL), recipient: newFundedClient(0) };
}

export default function ({ sender, recipient }) {
  const key = uuid();
  const first = transfer(sender, recipient.iban, AMOUNT, key);
  if (first.status !== 201 && first.status !== 422) {
    // Anything else is worth a line in the log: which status, and the network error if there was no response.
    console.warn(`unexpected answer ${first.status} ${first.error || ''} ${first.body || ''}`);
  }

  if (first.status === 201) {
    // A client whose connection dropped would send the same request again. It must get the same payment back.
    const replay = transfer(sender, recipient.iban, AMOUNT, key);
    check(replay, {
      'replay returns the same payment': (r) => r.status === 201 && r.json('id') === first.json('id'),
    }, { kind: 'idempotency' });
  }
}

/** Runs once, after the load: what the accounts hold must add up exactly. */
export function teardown({ sender, recipient }) {
  const senderBalance = account(sender).json('balance');
  const received = account(recipient).json('balance');
  const transfers = Math.round(received / AMOUNT);

  console.log(`transfers that went through: ${transfers}, sender left with ${senderBalance}, recipient got ${received}`);

  check(null, {
    'the sender never went below zero': () => senderBalance >= 0,
    'every transfer took exactly amount + fee': () =>
      Math.abs(senderBalance - (INITIAL - transfers * (AMOUNT + FEE))) < 0.001,
    'the money ran out, so the limit was really tested': () => senderBalance < AMOUNT + FEE,
  }, { kind: 'ledger' });
}
