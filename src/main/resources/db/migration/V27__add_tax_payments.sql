-- The single tax of sole proprietors: a TAX_PAYMENT leaves the bank for the tax office (no account in the bank on
-- the other side), the client's consent to auto-pay, and per tax period what has been paid and which notices went out.

ALTER TABLE payments DROP CONSTRAINT ck_payments_type;
ALTER TABLE payments ADD CONSTRAINT ck_payments_type
    CHECK (type IN ('TOP_UP', 'TRANSFER', 'CARD_TRANSFER', 'BILL_PAYMENT', 'EXCHANGE',
                    'SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL', 'CREDIT_REPAYMENT', 'TAX_PAYMENT'));

ALTER TABLE payments DROP CONSTRAINT ck_payments_recipient;
ALTER TABLE payments ADD CONSTRAINT ck_payments_recipient
    CHECK (type IN ('BILL_PAYMENT', 'TAX_PAYMENT') OR to_account_id IS NOT NULL);

ALTER TABLE businesses ADD COLUMN auto_pay_tax BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE tax_obligations
(
    id                    UUID PRIMARY KEY,
    business_id           UUID           NOT NULL REFERENCES businesses (id),
    -- "2026-11" for groups 1 and 2, "2026-Q4" for group 3.
    period                VARCHAR(7)     NOT NULL,
    paid_amount           NUMERIC(19, 4) NOT NULL,
    last_payment_id       UUID REFERENCES payments (id),
    reminder_sent_at      TIMESTAMPTZ,
    shortfall_notified_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_tax_obligations_business_period UNIQUE (business_id, period),
    CONSTRAINT ck_tax_obligations_period CHECK (period ~ '^[0-9]{4}-([0-9]{2}|Q[1-4])$'),
    CONSTRAINT ck_tax_obligations_paid CHECK (paid_amount >= 0)
);
