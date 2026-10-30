-- Credit line: a client's UAH account that may go below zero, down to its credit limit.
-- A negative balance is the client's debt to the bank; the bank's side of it is the money already paid out
-- (to a provider, to another card), so every currency still sums to zero.
ALTER TABLE accounts ADD COLUMN credit_limit NUMERIC(19, 4);

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type
    CHECK (type IN ('CLIENT', 'SAVINGS', 'CREDIT', 'SETTLEMENT', 'FEE_INCOME', 'HOLD', 'FX_POSITION',
                    'INTEREST_EXPENSE', 'CASHBACK_EXPENSE'));

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_client_has_owner;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_client_has_owner
    CHECK ((type IN ('CLIENT', 'SAVINGS', 'CREDIT')) = (owner_id IS NOT NULL));

-- Only a credit account has a limit, it is always in hryvnias, and its debt never goes past the limit.
-- A positive balance is allowed: a refund or cashback after the debt was repaid is the client's own money.
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_credit_limit
    CHECK ((type = 'CREDIT') = (credit_limit IS NOT NULL));
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_credit_limit_positive
    CHECK (credit_limit IS NULL OR credit_limit > 0);
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_credit_in_uah
    CHECK (type <> 'CREDIT' OR currency = 'UAH');
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_credit_within_limit
    CHECK (type <> 'CREDIT' OR balance >= -credit_limit);

DROP INDEX uq_accounts_bank_type_currency;
CREATE UNIQUE INDEX uq_accounts_bank_type_currency ON accounts (type, currency)
    WHERE type NOT IN ('CLIENT', 'SAVINGS', 'CREDIT');

-- One credit line per client.
CREATE UNIQUE INDEX uq_accounts_credit_owner ON accounts (owner_id) WHERE type = 'CREDIT';

ALTER TABLE payments DROP CONSTRAINT ck_payments_type;
ALTER TABLE payments ADD CONSTRAINT ck_payments_type
    CHECK (type IN ('TOP_UP', 'TRANSFER', 'CARD_TRANSFER', 'BILL_PAYMENT', 'EXCHANGE',
                    'SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL', 'CREDIT_REPAYMENT'));
