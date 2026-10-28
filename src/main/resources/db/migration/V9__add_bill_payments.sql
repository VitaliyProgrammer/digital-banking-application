-- HOLD: the bank's "money in transit" account. A bill payment first moves the client's money here,
-- then either on to the outside world (capture) or back to the client (release). Nothing is ever just "gone".
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type CHECK (type IN ('CLIENT', 'SETTLEMENT', 'FEE_INCOME', 'HOLD'));

INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-HOLD-UAH', 'UAH', 'HOLD', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-HOLD-USD', 'USD', 'HOLD', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-HOLD-EUR', 'EUR', 'HOLD', 'ACTIVE', 0, now());

-- The append-only trigger blocks UPDATE of rows, not changes of constraints, so this stays allowed.
ALTER TABLE ledger_transactions DROP CONSTRAINT ck_ledger_transactions_type;
ALTER TABLE ledger_transactions ADD CONSTRAINT ck_ledger_transactions_type
    CHECK (type IN ('DEPOSIT', 'TRANSFER', 'HOLD', 'CAPTURE', 'RELEASE'));

CREATE TABLE providers
(
    id       UUID PRIMARY KEY,
    code     VARCHAR(40)  NOT NULL,
    name     VARCHAR(100) NOT NULL,
    category VARCHAR(20)  NOT NULL,
    currency VARCHAR(3)   NOT NULL,
    active   BOOLEAN      NOT NULL,

    CONSTRAINT uq_providers_code UNIQUE (code),
    CONSTRAINT ck_providers_category
        CHECK (category IN ('ELECTRICITY', 'WATER', 'GAS', 'HEATING', 'INTERNET', 'MOBILE')),
    CONSTRAINT ck_providers_currency CHECK (currency IN ('UAH', 'USD', 'EUR'))
);

-- Fictional providers. A mobile operator is just another provider: same flow, the "account" is a phone number.
INSERT INTO providers (id, code, name, category, currency, active)
VALUES (gen_random_uuid(), 'city-power', 'Міське світло (demo)', 'ELECTRICITY', 'UAH', TRUE),
       (gen_random_uuid(), 'city-water', 'Міська вода (demo)', 'WATER', 'UAH', TRUE),
       (gen_random_uuid(), 'gas-network', 'Газмережа (demo)', 'GAS', 'UAH', TRUE),
       (gen_random_uuid(), 'heat-supply', 'Теплопостач (demo)', 'HEATING', 'UAH', TRUE),
       (gen_random_uuid(), 'fiber-net', 'ОптикаНет (demo)', 'INTERNET', 'UAH', TRUE),
       (gen_random_uuid(), 'mobile-one', 'Мобільний Один (demo)', 'MOBILE', 'UAH', TRUE);

ALTER TABLE payments ADD COLUMN provider_id UUID REFERENCES providers (id);
ALTER TABLE payments ADD COLUMN provider_account_number VARCHAR(40);
ALTER TABLE payments ADD COLUMN hold_transaction_id UUID REFERENCES ledger_transactions (id);
ALTER TABLE payments ADD COLUMN attempts INT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN next_attempt_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN failure_reason VARCHAR(255);

-- A bill payment has no recipient account inside the bank: the money leaves to a provider.
ALTER TABLE payments ALTER COLUMN to_account_id DROP NOT NULL;

ALTER TABLE payments DROP CONSTRAINT ck_payments_type;
ALTER TABLE payments ADD CONSTRAINT ck_payments_type
    CHECK (type IN ('TOP_UP', 'TRANSFER', 'CARD_TRANSFER', 'BILL_PAYMENT'));

ALTER TABLE payments DROP CONSTRAINT ck_payments_status;
ALTER TABLE payments ADD CONSTRAINT ck_payments_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'UNKNOWN', 'COMPLETED', 'FAILED'));

-- Was "COMPLETED if and only if posted"; a bill payment has a posted hold long before it completes.
ALTER TABLE payments DROP CONSTRAINT ck_payments_completed_has_ledger_transaction;
ALTER TABLE payments ADD CONSTRAINT ck_payments_finished_has_ledger_transaction
    CHECK (status NOT IN ('COMPLETED', 'FAILED') OR (ledger_transaction_id IS NOT NULL AND completed_at IS NOT NULL));

ALTER TABLE payments ADD CONSTRAINT ck_payments_recipient
    CHECK (type = 'BILL_PAYMENT' OR to_account_id IS NOT NULL);
ALTER TABLE payments ADD CONSTRAINT ck_payments_bill_has_provider
    CHECK ((type = 'BILL_PAYMENT') = (provider_id IS NOT NULL AND provider_account_number IS NOT NULL));

-- The worker looks for due payments every few seconds; only unfinished bill payments need to be found fast.
CREATE INDEX ix_payments_due ON payments (status, next_attempt_at) WHERE status IN ('PROCESSING', 'UNKNOWN');
