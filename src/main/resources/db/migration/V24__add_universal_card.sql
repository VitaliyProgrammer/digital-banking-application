-- UNIVERSAL: the bank's main card for everyday payments and payouts (a credit limit will live on it as well).
ALTER TABLE cards DROP CONSTRAINT ck_cards_type;
ALTER TABLE cards ADD CONSTRAINT ck_cards_type CHECK (type IN ('UNIVERSAL', 'PAYROLL', 'STUDENT', 'VIRTUAL', 'CREDIT'));
