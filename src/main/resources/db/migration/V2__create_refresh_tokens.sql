CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

-- Only active sessions are looked up by user (revoke all on token reuse), so the index skips revoked rows.
CREATE INDEX ix_refresh_tokens_active_user_id ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
