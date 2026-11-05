-- Sole proprietors (ФОП): a business account and a tax jar next to the person's own accounts, a BUSINESS card,
-- the profile they registered with, and every income with the tax put aside from it.

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type
    CHECK (type IN ('CLIENT', 'SAVINGS', 'CREDIT', 'BUSINESS', 'TAX_RESERVE', 'SETTLEMENT', 'FEE_INCOME', 'HOLD',
                    'FX_POSITION', 'INTEREST_EXPENSE', 'CASHBACK_EXPENSE', 'INTEREST_INCOME'));

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_client_has_owner;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_client_has_owner
    CHECK ((type IN ('CLIENT', 'SAVINGS', 'CREDIT', 'BUSINESS', 'TAX_RESERVE')) = (owner_id IS NOT NULL));

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_client_balance_non_negative;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_client_balance_non_negative
    CHECK (type NOT IN ('CLIENT', 'SAVINGS', 'BUSINESS', 'TAX_RESERVE') OR balance >= 0 OR credit_limit IS NOT NULL);

DROP INDEX uq_accounts_bank_type_currency_shard;
CREATE UNIQUE INDEX uq_accounts_bank_type_currency_shard ON accounts (type, currency, shard)
    WHERE type NOT IN ('CLIENT', 'SAVINGS', 'CREDIT', 'BUSINESS', 'TAX_RESERVE');

-- One business account and one tax jar per client.
CREATE UNIQUE INDEX uq_accounts_business_owner ON accounts (owner_id) WHERE type = 'BUSINESS';
CREATE UNIQUE INDEX uq_accounts_tax_reserve_owner ON accounts (owner_id) WHERE type = 'TAX_RESERVE';

ALTER TABLE cards DROP CONSTRAINT ck_cards_type;
ALTER TABLE cards ADD CONSTRAINT ck_cards_type
    CHECK (type IN ('UNIVERSAL', 'BUSINESS', 'PAYROLL', 'STUDENT', 'VIRTUAL', 'CREDIT'));

CREATE TABLE businesses
(
    id                     UUID PRIMARY KEY,
    owner_id               UUID         NOT NULL REFERENCES users (id),
    name                   VARCHAR(150) NOT NULL,
    tax_id                 VARCHAR(10)  NOT NULL,
    kved_codes             VARCHAR(100) NOT NULL,
    tax_group              VARCHAR(10)  NOT NULL,
    vat_payer              BOOLEAN      NOT NULL,
    registered_on          DATE         NOT NULL,
    business_account_id    UUID         NOT NULL REFERENCES accounts (id),
    tax_reserve_account_id UUID         NOT NULL REFERENCES accounts (id),
    created_at             TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_businesses_owner UNIQUE (owner_id),
    CONSTRAINT uq_businesses_tax_id UNIQUE (tax_id),
    CONSTRAINT uq_businesses_business_account UNIQUE (business_account_id),
    CONSTRAINT ck_businesses_tax_id CHECK (tax_id ~ '^[0-9]{10}$'),
    CONSTRAINT ck_businesses_tax_group CHECK (tax_group IN ('GROUP_1', 'GROUP_2', 'GROUP_3')),
    -- By the Tax Code only group 3 may also pay VAT.
    CONSTRAINT ck_businesses_vat_group_3 CHECK (NOT vat_payer OR tax_group = 'GROUP_3')
);

CREATE TABLE business_incomes
(
    id          UUID PRIMARY KEY,
    business_id UUID           NOT NULL REFERENCES businesses (id),
    payment_id  UUID           NOT NULL REFERENCES payments (id),
    amount      NUMERIC(19, 4) NOT NULL,
    tax         NUMERIC(19, 4) NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_business_incomes_payment UNIQUE (payment_id),
    CONSTRAINT ck_business_incomes_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_business_incomes_tax CHECK (tax >= 0 AND tax <= amount)
);

CREATE INDEX ix_business_incomes_business_created ON business_incomes (business_id, created_at);
