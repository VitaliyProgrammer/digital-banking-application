ALTER TABLE payments ADD COLUMN from_card_id UUID REFERENCES cards (id);
ALTER TABLE payments ADD COLUMN to_card_id UUID REFERENCES cards (id);

ALTER TABLE payments DROP CONSTRAINT ck_payments_type;
ALTER TABLE payments ADD CONSTRAINT ck_payments_type CHECK (type IN ('TOP_UP', 'TRANSFER', 'CARD_TRANSFER'));

ALTER TABLE payments ADD CONSTRAINT ck_payments_card_transfer_has_cards
    CHECK ((type = 'CARD_TRANSFER') = (from_card_id IS NOT NULL AND to_card_id IS NOT NULL));
