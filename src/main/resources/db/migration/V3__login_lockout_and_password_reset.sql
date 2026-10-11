-- Login lockout: count consecutive failed logins; lock the account until locked_until when the limit is hit.
ALTER TABLE users
    ADD COLUMN failed_login_attempts INT         NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMPTZ;

-- Forgot password: single-use tokens sent by email. Only a SHA-256 hash is stored,
-- so a leaked database cannot be used to reset anyone's password.
CREATE TABLE password_reset_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX password_reset_tokens_hash_uk ON password_reset_tokens (token_hash);
