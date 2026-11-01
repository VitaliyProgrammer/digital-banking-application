-- Hot accounts. Every transfer with a fee locks the bank's ONE fee income row, every top-up the ONE settlement row:
-- under load all those transactions queue behind a single row lock (found with k6, see load-tests/README.md).
-- The cure used by real ledgers: split each hot bank account into shards and post to a random one. The bank's
-- fee income is then the sum of its shards; every posting is still balanced, so nothing else changes.
ALTER TABLE accounts ADD COLUMN shard SMALLINT NOT NULL DEFAULT 0;

DROP INDEX uq_accounts_bank_type_currency;
CREATE UNIQUE INDEX uq_accounts_bank_type_currency_shard ON accounts (type, currency, shard)
    WHERE type NOT IN ('CLIENT', 'SAVINGS', 'CREDIT');

-- The existing account of each type is shard 0; add shards 1..15, so 16 per type and currency.
INSERT INTO accounts (id, owner_id, number, currency, type, status, balance, created_at, shard)
SELECT gen_random_uuid(),
       NULL,
       'BANK-' || replace(hot.type, '_', '-') || '-' || currency.code || '-' || shard,
       currency.code,
       hot.type,
       'ACTIVE',
       0,
       now(),
       shard
FROM (VALUES ('FEE_INCOME'), ('SETTLEMENT'), ('HOLD')) AS hot (type)
         CROSS JOIN (VALUES ('UAH'), ('USD'), ('EUR')) AS currency (code)
         CROSS JOIN generate_series(1, 15) AS shard;
