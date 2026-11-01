-- Who did what to what, and when: every action of a bank employee that changes something or looks at a client's data.
-- Written in the same transaction as the action itself, so an entry exists if and only if the action happened.
CREATE TABLE audit_log
(
    id          UUID PRIMARY KEY,
    actor_id    UUID        NOT NULL REFERENCES users (id),
    -- The role at the moment of the action: a later promotion or demotion must not rewrite the past.
    actor_role  VARCHAR(20) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id   UUID        NOT NULL,
    -- What exactly happened: from which state to which, the amounts, the comment.
    details     JSONB       NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_audit_log_occurred ON audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_id, occurred_at DESC);
CREATE INDEX ix_audit_log_target ON audit_log (target_type, target_id, occurred_at DESC);

-- A log that can be edited is not a log: the same guard as the ledger.
CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE
    ON audit_log
    FOR EACH ROW
EXECUTE FUNCTION forbid_ledger_change();
