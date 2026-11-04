-- "Forgot password": a one-time link sent by email. Only the SHA-256 of the token is stored, so a stolen table
-- holds no working links; the raw token exists only in the email.
CREATE TABLE password_reset_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    -- Set when the link is used, or when a newer request replaces it: a link works once.
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_id) WHERE used_at IS NULL;
