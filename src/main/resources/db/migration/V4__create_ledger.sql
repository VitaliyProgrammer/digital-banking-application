CREATE TABLE ledger_transactions
(
    id          UUID PRIMARY KEY,
    type        VARCHAR(30)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_ledger_transactions_type CHECK (type IN ('DEPOSIT', 'TRANSFER'))
);

CREATE TABLE ledger_entries
(
    id             UUID PRIMARY KEY,
    transaction_id UUID           NOT NULL REFERENCES ledger_transactions (id),
    account_id     UUID           NOT NULL REFERENCES accounts (id),
    direction      VARCHAR(6)     NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    balance_after  NUMERIC(19, 4) NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,

    CONSTRAINT ck_ledger_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0)
);

-- Statement: entries of one account, newest first.
CREATE INDEX ix_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC);
CREATE INDEX ix_ledger_entries_transaction ON ledger_entries (transaction_id);

-- The ledger is append-only: a mistake is corrected by a new compensating transaction, never by editing history.
-- Enforced by the database itself, so neither a bug nor a manual SQL session can rewrite it.
CREATE FUNCTION forbid_ledger_change() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'Ledger is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_transactions_append_only
    BEFORE UPDATE OR DELETE
    ON ledger_transactions
    FOR EACH ROW
EXECUTE FUNCTION forbid_ledger_change();

CREATE TRIGGER trg_ledger_entries_append_only
    BEFORE UPDATE OR DELETE
    ON ledger_entries
    FOR EACH ROW
EXECUTE FUNCTION forbid_ledger_change();
