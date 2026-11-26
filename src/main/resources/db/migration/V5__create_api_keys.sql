CREATE TABLE api_keys (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL REFERENCES users (id),
    name          VARCHAR(100)  NOT NULL,
    key_hash      VARCHAR(64)   NOT NULL,
    scopes        VARCHAR(200)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX api_keys_hash_uk ON api_keys (key_hash);
CREATE INDEX api_keys_user_idx ON api_keys (user_id);
