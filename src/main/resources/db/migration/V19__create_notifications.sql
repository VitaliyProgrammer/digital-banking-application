-- What the client sees under the bell in the app. Made from the bank's events by the notification consumer.
CREATE TABLE notifications
(
    id              UUID PRIMARY KEY,
    user_id         UUID          NOT NULL REFERENCES users (id),
    -- The outbox event (= RabbitMQ message id) this notification was made from. Delivery is at least once,
    -- so the same event may arrive twice: the unique key turns the second one into a no-op.
    source_event_id UUID          NOT NULL,
    event_type      VARCHAR(50)   NOT NULL,
    title           VARCHAR(120)  NOT NULL,
    body            VARCHAR(1000) NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    read_at         TIMESTAMPTZ,

    -- The email copy is sent by its own job, not by the consumer: a mail server that is down
    -- must not push the message into retries and the dead-letter queue.
    email_status    VARCHAR(20)   NOT NULL,
    email_attempts  INT           NOT NULL DEFAULT 0,
    next_email_at   TIMESTAMPTZ,
    email_sent_at   TIMESTAMPTZ,
    email_error     VARCHAR(500),

    CONSTRAINT uq_notifications_source_event UNIQUE (source_event_id),
    CONSTRAINT ck_notifications_email_status CHECK (email_status IN ('NOT_NEEDED', 'PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_notifications_email_sent CHECK ((email_status = 'SENT') = (email_sent_at IS NOT NULL)),
    CONSTRAINT ck_notifications_email_due CHECK ((email_status = 'PENDING') = (next_email_at IS NOT NULL))
);

-- The bell: newest first, and the unread count.
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;

-- The email job's only question: what is due?
CREATE INDEX ix_notifications_email_due ON notifications (next_email_at) WHERE email_status = 'PENDING';
