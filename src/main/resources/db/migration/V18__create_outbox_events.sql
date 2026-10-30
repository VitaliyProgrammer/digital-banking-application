-- Transactional outbox: an event is written in the same transaction as the business change it describes,
-- so it exists if and only if that change committed. A relay publishes it to RabbitMQ afterwards.
CREATE TABLE outbox_events
(
    id              UUID PRIMARY KEY,
    -- Also the RabbitMQ routing key, e.g. "payment.completed".
    event_type      VARCHAR(50)  NOT NULL,
    -- What the event is about (a payment, an application, a statement): consumers group and deduplicate by it.
    aggregate_id    UUID         NOT NULL,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    published_at    TIMESTAMPTZ,
    attempts        INT          NOT NULL DEFAULT 0,
    -- After a failed publish the relay waits longer each time instead of hammering a broker that is down.
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      VARCHAR(500),

    CONSTRAINT ck_outbox_events_attempts CHECK (attempts >= 0)
);

-- The relay's only question: "what is due and not yet published?" A partial index stays small, because
-- published events (almost all of them) are not in it.
CREATE INDEX ix_outbox_events_due ON outbox_events (next_attempt_at, created_at) WHERE published_at IS NULL;
