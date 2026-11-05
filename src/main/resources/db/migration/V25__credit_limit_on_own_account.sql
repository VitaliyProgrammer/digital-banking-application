-- The credit limit now lives on the client's own UAH account, the one with the UNIVERSAL card (like monobank):
-- above zero is the client's money, below zero the debt. Separate CREDIT accounts of the earlier model keep working:
-- they are simply accounts with a limit too, so the same code serves both.

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_credit_limit;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_credit_limit
    CHECK ((type <> 'CREDIT' OR credit_limit IS NOT NULL) AND (credit_limit IS NULL OR type IN ('CLIENT', 'CREDIT')));

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_credit_in_uah;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_credit_in_uah
    CHECK (credit_limit IS NULL OR currency = 'UAH');

-- A current account may go below zero only if it has a limit. How far is checked in Java under the row lock,
-- because the bank's interest may pass the limit (the same reason V17 dropped the CHECK on CREDIT accounts).
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_client_balance_non_negative;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_client_balance_non_negative
    CHECK (type NOT IN ('CLIENT', 'SAVINGS') OR balance >= 0 OR credit_limit IS NOT NULL);

-- Still one credit line per client, whichever account carries it.
DROP INDEX uq_accounts_credit_owner;
CREATE UNIQUE INDEX uq_accounts_credit_owner ON accounts (owner_id) WHERE credit_limit IS NOT NULL;

ALTER TABLE ledger_transactions DROP CONSTRAINT ck_ledger_transactions_type;
ALTER TABLE ledger_transactions ADD CONSTRAINT ck_ledger_transactions_type
    CHECK (type IN ('DEPOSIT', 'TRANSFER', 'CARD_TRANSFER', 'HOLD', 'CAPTURE', 'RELEASE', 'EXCHANGE', 'INTEREST',
                    'CASHBACK', 'CREDIT_CHARGES'));
