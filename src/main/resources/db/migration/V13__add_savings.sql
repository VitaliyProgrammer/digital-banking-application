-- Savings: a client's second account per currency, "a jar" that earns interest.
-- INTEREST_EXPENSE: the bank's cost of paying that interest; money for it comes from here, so it goes negative.
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type
    CHECK (type IN ('CLIENT', 'SAVINGS', 'SETTLEMENT', 'FEE_INCOME', 'HOLD', 'FX_POSITION', 'INTEREST_EXPENSE'));

-- A savings account belongs to a client exactly like a current account, and can never go below zero either.
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_client_has_owner;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_client_has_owner
    CHECK ((type IN ('CLIENT', 'SAVINGS')) = (owner_id IS NOT NULL));

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_client_balance_non_negative;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_client_balance_non_negative
    CHECK (type NOT IN ('CLIENT', 'SAVINGS') OR balance >= 0);

-- "One bank account of each type per currency" must not count the clients' savings accounts.
DROP INDEX uq_accounts_bank_type_currency;
CREATE UNIQUE INDEX uq_accounts_bank_type_currency ON accounts (type, currency) WHERE type NOT IN ('CLIENT', 'SAVINGS');

CREATE UNIQUE INDEX uq_accounts_savings_currency ON accounts (owner_id, currency) WHERE type = 'SAVINGS';

INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-INTEREST-EXPENSE-UAH', 'UAH', 'INTEREST_EXPENSE', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-INTEREST-EXPENSE-USD', 'USD', 'INTEREST_EXPENSE', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-INTEREST-EXPENSE-EUR', 'EUR', 'INTEREST_EXPENSE', 'ACTIVE', 0, now());

ALTER TABLE ledger_transactions DROP CONSTRAINT ck_ledger_transactions_type;
ALTER TABLE ledger_transactions ADD CONSTRAINT ck_ledger_transactions_type
    CHECK (type IN ('DEPOSIT', 'TRANSFER', 'HOLD', 'CAPTURE', 'RELEASE', 'EXCHANGE', 'INTEREST'));

ALTER TABLE payments DROP CONSTRAINT ck_payments_type;
ALTER TABLE payments ADD CONSTRAINT ck_payments_type
    CHECK (type IN ('TOP_UP', 'TRANSFER', 'CARD_TRANSFER', 'BILL_PAYMENT', 'EXCHANGE',
                    'SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL'));

-- Interest earned by one savings account for one day. Not money yet: kept with 8 decimals, outside the ledger,
-- because a day's interest on a small balance is a fraction of a kopiyka. The ledger only sees the monthly payout.
CREATE TABLE interest_accruals
(
    id           UUID PRIMARY KEY,
    account_id   UUID           NOT NULL REFERENCES accounts (id),
    accrual_date DATE           NOT NULL,
    balance      NUMERIC(19, 4) NOT NULL,
    annual_rate  NUMERIC(5, 2)  NOT NULL,
    amount       NUMERIC(19, 8) NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL,

    -- A day is accrued once, even if the job runs twice or on two instances (ON CONFLICT DO NOTHING).
    CONSTRAINT uq_interest_accruals_account_date UNIQUE (account_id, accrual_date),
    CONSTRAINT ck_interest_accruals_amount_positive CHECK (amount > 0)
);

-- What was actually paid for a month. Paid = accrued so far, rounded down to kopiyky; the rest waits for next month.
CREATE TABLE interest_payouts
(
    id                    UUID PRIMARY KEY,
    account_id            UUID           NOT NULL REFERENCES accounts (id),
    period                VARCHAR(7)     NOT NULL,
    amount                NUMERIC(19, 4) NOT NULL,
    ledger_transaction_id UUID REFERENCES ledger_transactions (id),
    paid_at               TIMESTAMPTZ    NOT NULL,

    -- A month is paid once: the row is inserted before the money moves, so a second job stops right here.
    CONSTRAINT uq_interest_payouts_account_period UNIQUE (account_id, period),
    CONSTRAINT ck_interest_payouts_amount_positive CHECK (amount > 0)
);
