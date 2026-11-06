-- The bank's buy/sell rates are quoted in kopiykas, like on any exchange board: 41.85, not 41.8508.
-- Existing rows are rounded in the bank's favour, the same rule the code uses: buy down, sell up
-- (so buy < sell still holds). The NBU rate keeps its own precision: it is official data, not ours.
ALTER TABLE bank_rates ALTER COLUMN buy TYPE NUMERIC(19, 2) USING floor(buy * 100) / 100;
ALTER TABLE bank_rates ALTER COLUMN sell TYPE NUMERIC(19, 2) USING ceil(sell * 100) / 100;
