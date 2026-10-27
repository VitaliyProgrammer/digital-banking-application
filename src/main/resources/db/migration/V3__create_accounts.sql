CREATE TABLE accounts
(
    id         UUID PRIMARY KEY,
    owner_id   UUID REFERENCES users (id),
    number     VARCHAR(34)    NOT NULL,
    currency   VARCHAR(3)     NOT NULL,
    type       VARCHAR(20)    NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    balance    NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_accounts_number UNIQUE (number),
    CONSTRAINT ck_accounts_currency CHECK (currency IN ('UAH', 'USD', 'EUR')),
    CONSTRAINT ck_accounts_type CHECK (type IN ('CLIENT', 'SETTLEMENT')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    -- A client account always has an owner; bank accounts never do.
    CONSTRAINT ck_accounts_client_has_owner CHECK ((type = 'CLIENT') = (owner_id IS NOT NULL)),
    -- Last line of defence: a client balance can never go below zero, whatever the Java code does.
    CONSTRAINT ck_accounts_client_balance_non_negative CHECK (type <> 'CLIENT' OR balance >= 0)
);

-- One client account per currency, like one UAH card and one USD card in a mobile bank.
CREATE UNIQUE INDEX uq_accounts_client_currency ON accounts (owner_id, currency) WHERE type = 'CLIENT';

-- Exactly one bank account of each type per currency.
CREATE UNIQUE INDEX uq_accounts_bank_type_currency ON accounts (type, currency) WHERE type <> 'CLIENT';

-- Settlement accounts represent the outside world: money that enters the bank is debited from them,
-- so their balance goes negative. The sum of all balances in a currency is therefore always zero.
INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-SETTLEMENT-UAH', 'UAH', 'SETTLEMENT', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-SETTLEMENT-USD', 'USD', 'SETTLEMENT', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-SETTLEMENT-EUR', 'EUR', 'SETTLEMENT', 'ACTIVE', 0, now());
