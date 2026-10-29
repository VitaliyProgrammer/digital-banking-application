-- FX position: the bank's side of every exchange. A client selling 100 USD for 4085.08 UAH gives the bank
-- +100 USD and -4085.08 UAH here, so each currency stays balanced on its own. The positions show how much
-- of each currency the bank has bought or sold; their value in UAH changes with the rate, that is FX risk.
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type
    CHECK (type IN ('CLIENT', 'SETTLEMENT', 'FEE_INCOME', 'HOLD', 'FX_POSITION'));

INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-FX-POSITION-UAH', 'UAH', 'FX_POSITION', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-FX-POSITION-USD', 'USD', 'FX_POSITION', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-FX-POSITION-EUR', 'EUR', 'FX_POSITION', 'ACTIVE', 0, now());

ALTER TABLE ledger_transactions DROP CONSTRAINT ck_ledger_transactions_type;
ALTER TABLE ledger_transactions ADD CONSTRAINT ck_ledger_transactions_type
    CHECK (type IN ('DEPOSIT', 'TRANSFER', 'HOLD', 'CAPTURE', 'RELEASE', 'EXCHANGE'));

-- A price the bank promised for a short time. The client sees exactly what they will get before agreeing.
CREATE TABLE exchange_quotes
(
    id              UUID PRIMARY KEY,
    owner_id        UUID           NOT NULL REFERENCES users (id),
    from_account_id UUID           NOT NULL REFERENCES accounts (id),
    to_account_id   UUID           NOT NULL REFERENCES accounts (id),
    from_currency   VARCHAR(3)     NOT NULL,
    to_currency     VARCHAR(3)     NOT NULL,
    sell_amount     NUMERIC(19, 4) NOT NULL,
    buy_amount      NUMERIC(19, 4) NOT NULL,
    -- Units of to_currency per 1 unit of from_currency, before the final rounding of buy_amount.
    rate            NUMERIC(19, 8) NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    expires_at      TIMESTAMPTZ    NOT NULL,

    CONSTRAINT ck_exchange_quotes_currencies CHECK (from_currency <> to_currency),
    CONSTRAINT ck_exchange_quotes_amounts_positive CHECK (sell_amount > 0 AND buy_amount > 0),
    CONSTRAINT ck_exchange_quotes_expiry CHECK (expires_at > created_at)
);

ALTER TABLE payments ADD COLUMN quote_id UUID REFERENCES exchange_quotes (id);
ALTER TABLE payments ADD COLUMN credited_amount NUMERIC(19, 4);
ALTER TABLE payments ADD COLUMN exchange_rate NUMERIC(19, 8);

-- A quote can be spent once, even by two requests with different idempotency keys.
ALTER TABLE payments ADD CONSTRAINT uq_payments_quote UNIQUE (quote_id);

ALTER TABLE payments DROP CONSTRAINT ck_payments_type;
ALTER TABLE payments ADD CONSTRAINT ck_payments_type
    CHECK (type IN ('TOP_UP', 'TRANSFER', 'CARD_TRANSFER', 'BILL_PAYMENT', 'EXCHANGE'));

ALTER TABLE payments ADD CONSTRAINT ck_payments_exchange_has_quote
    CHECK ((type = 'EXCHANGE') = (quote_id IS NOT NULL AND credited_amount IS NOT NULL AND exchange_rate IS NOT NULL));
