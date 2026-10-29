-- Human-readable receipt numbers like R-2026-00001234. A sequence, not max(number) + 1:
-- two parallel requests can never get the same number, and nothing has to be locked.
-- A sequence may skip numbers (a rolled back transaction keeps its value). For receipts that is fine:
-- the number must be unique and easy to read, not gapless.
CREATE SEQUENCE receipt_number_seq START WITH 1;

-- One receipt per payment, issued the first time the client asks for it.
CREATE TABLE receipts
(
    payment_id UUID PRIMARY KEY REFERENCES payments (id),
    number     VARCHAR(20) NOT NULL,
    issued_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_receipts_number UNIQUE (number)
);
