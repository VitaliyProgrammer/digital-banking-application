CREATE TABLE payments
(
    id                    UUID PRIMARY KEY,
    initiator_id          UUID           NOT NULL REFERENCES users (id),
    idempotency_key       VARCHAR(64)    NOT NULL,
    request_hash          VARCHAR(64)    NOT NULL,
    type                  VARCHAR(20)    NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    from_account_id       UUID REFERENCES accounts (id),
    to_account_id         UUID           NOT NULL REFERENCES accounts (id),
    currency              VARCHAR(3)     NOT NULL,
    amount                NUMERIC(19, 4) NOT NULL,
    fee                   NUMERIC(19, 4) NOT NULL,
    description           VARCHAR(255),
    ledger_transaction_id UUID REFERENCES ledger_transactions (id),
    created_at            TIMESTAMPTZ    NOT NULL,
    completed_at          TIMESTAMPTZ,

    -- The idempotency guard: the same user cannot create two payments with the same key.
    -- A parallel duplicate waits on this index until the first transaction commits, then fails.
    CONSTRAINT uq_payments_initiator_idempotency_key UNIQUE (initiator_id, idempotency_key),
    CONSTRAINT ck_payments_type CHECK (type IN ('TOP_UP', 'TRANSFER')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payments_fee_non_negative CHECK (fee >= 0),
    CONSTRAINT ck_payments_top_up_has_no_source CHECK ((type = 'TOP_UP') = (from_account_id IS NULL)),
    CONSTRAINT ck_payments_completed_has_ledger_transaction
        CHECK ((status = 'COMPLETED') = (ledger_transaction_id IS NOT NULL AND completed_at IS NOT NULL))
);
