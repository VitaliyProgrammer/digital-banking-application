-- INTEREST_INCOME: what the bank earns on credit lines. Credit is in hryvnias only, so one account is enough.
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type
    CHECK (type IN ('CLIENT', 'SAVINGS', 'CREDIT', 'SETTLEMENT', 'FEE_INCOME', 'HOLD', 'FX_POSITION',
                    'INTEREST_EXPENSE', 'CASHBACK_EXPENSE', 'INTEREST_INCOME'));

INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-INTEREST-INCOME-UAH', 'UAH', 'INTEREST_INCOME', 'ACTIVE', 0, now());

-- Interest and a late fee are charged even on a credit line that is used up: the debt may grow past the limit
-- (an "overlimit"). Spending still stops at the limit; that rule lives in Account.apply(), under the row lock,
-- and only CREDIT_CHARGES may pass it, so the database can no longer state it as a plain CHECK.
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_credit_within_limit;

ALTER TABLE ledger_transactions DROP CONSTRAINT ck_ledger_transactions_type;
ALTER TABLE ledger_transactions ADD CONSTRAINT ck_ledger_transactions_type
    CHECK (type IN ('DEPOSIT', 'TRANSFER', 'HOLD', 'CAPTURE', 'RELEASE', 'EXCHANGE', 'INTEREST', 'CASHBACK',
                    'CREDIT_CHARGES'));

-- One month of a credit line, closed at midnight on the 1st (Kyiv). The debt at that moment must be repaid
-- by the end of the next month: in full to pay no interest, at least the minimum payment to avoid a late fee.
CREATE TABLE credit_statements
(
    id                    UUID PRIMARY KEY,
    account_id            UUID           NOT NULL REFERENCES accounts (id),
    period                VARCHAR(7)     NOT NULL,
    closing_debt          NUMERIC(19, 2) NOT NULL,
    minimum_payment       NUMERIC(19, 2) NOT NULL,
    closed_at             TIMESTAMPTZ    NOT NULL,
    due_date              DATE           NOT NULL,
    -- The first moment after the due date (midnight that ends it): repayments before it count.
    due_at                TIMESTAMPTZ    NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    repaid                NUMERIC(19, 2),
    interest              NUMERIC(19, 2),
    late_fee              NUMERIC(19, 2),
    ledger_transaction_id UUID REFERENCES ledger_transactions (id),
    settled_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ    NOT NULL,

    -- A month gets one statement, even if the job runs twice or on two instances (ON CONFLICT DO NOTHING).
    CONSTRAINT uq_credit_statements_account_period UNIQUE (account_id, period),
    CONSTRAINT ck_credit_statements_status CHECK (status IN ('OPEN', 'PAID', 'MINIMUM_PAID', 'OVERDUE')),
    CONSTRAINT ck_credit_statements_debt_positive CHECK (closing_debt > 0),
    CONSTRAINT ck_credit_statements_minimum CHECK (minimum_payment > 0 AND minimum_payment <= closing_debt),
    -- Settled means all of the outcome is written down; open means none of it is.
    CONSTRAINT ck_credit_statements_settled CHECK (
        (status = 'OPEN') = (settled_at IS NULL)
            AND (settled_at IS NULL) = (repaid IS NULL)
            AND (settled_at IS NULL) = (interest IS NULL)
            AND (settled_at IS NULL) = (late_fee IS NULL))
);

-- The job looks for open statements whose due date has passed.
CREATE INDEX ix_credit_statements_open_due ON credit_statements (account_id, due_at) WHERE status = 'OPEN';
