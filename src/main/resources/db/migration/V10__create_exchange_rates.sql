-- Official NBU rates: one row per currency per day, never changed afterwards. This is the history for charts.
-- Rates are "UAH per 1 unit of the currency", e.g. USD 41.4729.
CREATE TABLE official_rates
(
    id         UUID PRIMARY KEY,
    currency   VARCHAR(3)     NOT NULL,
    rate_date  DATE           NOT NULL,
    rate       NUMERIC(19, 6) NOT NULL,
    fetched_at TIMESTAMPTZ    NOT NULL,

    -- Lets several application instances save the same day's rate without duplicates (ON CONFLICT DO NOTHING).
    CONSTRAINT uq_official_rates_currency_date UNIQUE (currency, rate_date),
    CONSTRAINT ck_official_rates_currency CHECK (currency IN ('USD', 'EUR')),
    CONSTRAINT ck_official_rates_positive CHECK (rate > 0)
);

-- The bank's own buy/sell rates. Append-only: a change is a new row, so the table is also the change history
-- with the author. The current rate of a currency is its newest row.
CREATE TABLE bank_rates
(
    id             UUID PRIMARY KEY,
    currency       VARCHAR(3)     NOT NULL,
    buy            NUMERIC(19, 4) NOT NULL,
    sell           NUMERIC(19, 4) NOT NULL,
    mode           VARCHAR(10)    NOT NULL,
    spread_percent NUMERIC(5, 2),
    official_rate  NUMERIC(19, 6) NOT NULL,
    -- NULL = set by the system (default spread or recalculation after a new NBU rate).
    set_by         UUID REFERENCES users (id),
    comment        VARCHAR(200),
    valid_from     TIMESTAMPTZ    NOT NULL,

    CONSTRAINT ck_bank_rates_currency CHECK (currency IN ('USD', 'EUR')),
    CONSTRAINT ck_bank_rates_mode CHECK (mode IN ('SPREAD', 'MANUAL')),
    -- The bank buys cheaper than it sells; otherwise a client could make money by exchanging back and forth.
    CONSTRAINT ck_bank_rates_buy_below_sell CHECK (buy > 0 AND buy < sell),
    CONSTRAINT ck_bank_rates_spread_only_in_spread_mode CHECK ((mode = 'SPREAD') = (spread_percent IS NOT NULL))
);

CREATE INDEX ix_bank_rates_currency_valid_from ON bank_rates (currency, valid_from DESC);
