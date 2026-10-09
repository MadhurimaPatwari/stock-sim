-- Users: the people who log in. Money and holdings live elsewhere (portfolios + ledger).
CREATE TABLE users (
    id                  UUID         PRIMARY KEY,
    email               VARCHAR(254) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(100),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    reporting_currency  CHAR(3)      NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT users_status_valid    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT users_currency_format CHECK (reporting_currency ~ '^[A-Z]{3}$')
);

CREATE UNIQUE INDEX users_email_uk ON users (email);
