-- A client's request to the bank's support: OPEN (in the queue) → IN_PROGRESS (one employee owns it) → RESOLVED.
CREATE TABLE support_tickets
(
    id          UUID PRIMARY KEY,
    client_id   UUID         NOT NULL REFERENCES users (id),
    subject     VARCHAR(120) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    assignee_id UUID REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    resolved_at TIMESTAMPTZ,

    CONSTRAINT ck_support_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    -- In the queue means nobody owns it yet; taken or resolved means exactly one employee did.
    CONSTRAINT ck_support_tickets_assignee CHECK ((status = 'OPEN') = (assignee_id IS NULL)),
    CONSTRAINT ck_support_tickets_resolved CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL)),
    CONSTRAINT ck_support_tickets_not_own CHECK (assignee_id IS NULL OR assignee_id <> client_id)
);

-- The queue: oldest open tickets first.
CREATE INDEX ix_support_tickets_open ON support_tickets (created_at) WHERE status = 'OPEN';
CREATE INDEX ix_support_tickets_client ON support_tickets (client_id, created_at DESC);

-- The conversation. Messages are only ever added: what was said to a client stays as it was said.
CREATE TABLE ticket_messages
(
    id         UUID PRIMARY KEY,
    ticket_id  UUID          NOT NULL REFERENCES support_tickets (id),
    author_id  UUID          NOT NULL REFERENCES users (id),
    from_staff BOOLEAN       NOT NULL,
    body       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_ticket_messages_ticket ON ticket_messages (ticket_id, created_at);

CREATE TRIGGER trg_ticket_messages_append_only
    BEFORE UPDATE OR DELETE
    ON ticket_messages
    FOR EACH ROW
EXECUTE FUNCTION forbid_ledger_change();
