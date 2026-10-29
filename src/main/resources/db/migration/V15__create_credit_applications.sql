-- A client's request for a credit limit and the decision on it: automatic or made by a person.
CREATE TABLE credit_applications
(
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL REFERENCES users (id),
    requested_limit NUMERIC(19, 2) NOT NULL,
    -- What the scoring calculated as affordable: half of the average monthly income.
    affordable_limit NUMERIC(19, 2) NOT NULL,
    approved_limit  NUMERIC(19, 2),
    status          VARCHAR(20)    NOT NULL,
    decision_source VARCHAR(10),
    -- Why the scoring decided so, one reason per line: shown to the reviewer and kept for audit.
    reasons         VARCHAR(1000)  NOT NULL,
    decided_by      UUID REFERENCES users (id),
    decision_comment VARCHAR(500),
    created_at      TIMESTAMPTZ    NOT NULL,
    decided_at      TIMESTAMPTZ,

    CONSTRAINT ck_credit_applications_status CHECK (status IN ('MANUAL_REVIEW', 'APPROVED', 'DECLINED')),
    CONSTRAINT ck_credit_applications_source CHECK (decision_source IN ('AUTO', 'MANUAL')),
    CONSTRAINT ck_credit_applications_requested_positive CHECK (requested_limit > 0),
    -- Approved means a limit; anything else means none.
    CONSTRAINT ck_credit_applications_approved_has_limit
        CHECK ((status = 'APPROVED') = (approved_limit IS NOT NULL AND approved_limit > 0)),
    -- A finished decision always says who made it (AUTO or MANUAL) and when.
    CONSTRAINT ck_credit_applications_decided
        CHECK ((status = 'MANUAL_REVIEW') = (decision_source IS NULL AND decided_at IS NULL)),
    -- A person's decision always names that person.
    CONSTRAINT ck_credit_applications_manual_has_reviewer
        CHECK (decision_source IS DISTINCT FROM 'MANUAL' OR decided_by IS NOT NULL)
);

-- At most one application that is waiting for review or already gave a limit, per client.
-- Two parallel submissions cannot both get in; after a decline the client may apply again.
CREATE UNIQUE INDEX uq_credit_applications_one_open_per_user
    ON credit_applications (user_id) WHERE status IN ('MANUAL_REVIEW', 'APPROVED');

-- The reviewers' queue: oldest first.
CREATE INDEX ix_credit_applications_review_queue
    ON credit_applications (created_at) WHERE status = 'MANUAL_REVIEW';
