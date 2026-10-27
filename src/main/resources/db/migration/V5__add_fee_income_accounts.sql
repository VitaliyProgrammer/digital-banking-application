ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type CHECK (type IN ('CLIENT', 'SETTLEMENT', 'FEE_INCOME'));

-- Fees charged to clients are credited here: the bank's income in each currency.
INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at)
VALUES (gen_random_uuid(), NULL, 'BANK-FEE-INCOME-UAH', 'UAH', 'FEE_INCOME', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-FEE-INCOME-USD', 'USD', 'FEE_INCOME', 'ACTIVE', 0, now()),
       (gen_random_uuid(), NULL, 'BANK-FEE-INCOME-EUR', 'EUR', 'FEE_INCOME', 'ACTIVE', 0, now());
