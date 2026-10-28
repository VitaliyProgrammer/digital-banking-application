CREATE TABLE cards
(
    id              UUID PRIMARY KEY,
    account_id      UUID        NOT NULL REFERENCES accounts (id),
    owner_id        UUID        NOT NULL REFERENCES users (id),
    type            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    -- AES-256-GCM: Base64(IV || ciphertext || auth tag). The plain card number is never stored.
    pan_encrypted   VARCHAR(100) NOT NULL,
    -- HMAC-SHA256 of the card number with a secret key: finds a card by its number without decrypting all of them.
    pan_lookup_hash VARCHAR(64)  NOT NULL,
    pan_last_four   VARCHAR(4)   NOT NULL,
    expires_on      DATE         NOT NULL,
    -- HMAC-SHA256 of the PIN with a secret key; NULL until the client sets a PIN.
    pin_hash        VARCHAR(64),
    created_at      TIMESTAMPTZ  NOT NULL,
    -- There is deliberately no CVV column: the CVV is derived from the card number with a secret key when needed.

    CONSTRAINT uq_cards_pan_lookup_hash UNIQUE (pan_lookup_hash),
    CONSTRAINT ck_cards_type CHECK (type IN ('PAYROLL', 'STUDENT', 'VIRTUAL', 'CREDIT')),
    CONSTRAINT ck_cards_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'PIN_LOCKED', 'CLOSED'))
);

-- One open card of each type per account; closed cards stay for history.
CREATE UNIQUE INDEX uq_cards_account_type_open ON cards (account_id, type) WHERE status <> 'CLOSED';

CREATE INDEX ix_cards_owner_id ON cards (owner_id);
