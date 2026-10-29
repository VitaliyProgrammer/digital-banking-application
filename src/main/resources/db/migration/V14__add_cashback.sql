-- CASHBACK_EXPENSE: the bank's marketing cost. Every cashback payout is debited here, so it goes negative.
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type
    CHECK (type IN ('CLIENT', 'SAVINGS', 'SETTLEMENT', 'FEE_INCOME', 'HOLD', 'FX_POSITION', 'INTEREST_EXPENSE',
                    'CASHBACK_EXPENSE'));

INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-CASHBACK-EXPENSE-UAH', 'UAH', 'CASHBACK_EXPENSE', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-CASHBACK-EXPENSE-USD', 'USD', 'CASHBACK_EXPENSE', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-CASHBACK-EXPENSE-EUR', 'EUR', 'CASHBACK_EXPENSE', 'ACTIVE', 0, now());

ALTER TABLE ledger_transactions DROP CONSTRAINT ck_ledger_transactions_type;
ALTER TABLE ledger_transactions ADD CONSTRAINT ck_ledger_transactions_type
    CHECK (type IN ('DEPOSIT', 'TRANSFER', 'HOLD', 'CAPTURE', 'RELEASE', 'EXCHANGE', 'INTEREST', 'CASHBACK'));

-- Cashback earned for one completed bill payment. Promised, not paid yet: the money moves once a month.
CREATE TABLE cashback_accruals
(
    id             UUID PRIMARY KEY,
    user_id        UUID           NOT NULL REFERENCES users (id),
    payment_id     UUID           NOT NULL REFERENCES payments (id),
    account_id     UUID           NOT NULL REFERENCES accounts (id),
    category       VARCHAR(20)    NOT NULL,
    payment_amount NUMERIC(19, 4) NOT NULL,
    rate           NUMERIC(5, 2)  NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    period         VARCHAR(7)     NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,

    -- One payment earns cashback once.
    CONSTRAINT uq_cashback_accruals_payment UNIQUE (payment_id),
    CONSTRAINT ck_cashback_accruals_amount_positive CHECK (amount > 0)
);

-- The monthly cap is checked against this sum on every new accrual.
CREATE INDEX ix_cashback_accruals_user_period ON cashback_accruals (user_id, period);

CREATE TABLE cashback_payouts
(
    id                    UUID PRIMARY KEY,
    account_id            UUID           NOT NULL REFERENCES accounts (id),
    period                VARCHAR(7)     NOT NULL,
    amount                NUMERIC(19, 2) NOT NULL,
    ledger_transaction_id UUID REFERENCES ledger_transactions (id),
    paid_at               TIMESTAMPTZ    NOT NULL,

    -- A month is paid once: inserted before the money moves, so a second job run stops here.
    CONSTRAINT uq_cashback_payouts_account_period UNIQUE (account_id, period),
    CONSTRAINT ck_cashback_payouts_amount_positive CHECK (amount > 0)
);
