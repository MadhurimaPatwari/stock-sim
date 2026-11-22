-- Portfolios: where a user's cash (and later, stock positions) live. One per user for now,
-- created at signup in the user's reporting currency.
CREATE TABLE portfolios (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    currency    CHAR(3)      NOT NULL REFERENCES currencies (code),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX portfolios_user_idx ON portfolios (user_id);

-- Ledger: one immutable row per cash movement. The balance is SUM(amount); rows are never
-- updated or deleted, so the history always explains the balance.
-- NUMERIC, not FLOAT: exact decimal arithmetic for money. Scale 4 covers every currency we allow
-- (currencies.minor_units <= 4); each amount is stored with at most its currency's decimals.
CREATE TABLE ledger_entries (
    id            UUID           PRIMARY KEY,
    portfolio_id  UUID           NOT NULL REFERENCES portfolios (id),
    type          VARCHAR(20)    NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT ledger_entries_type_valid      CHECK (type IN ('DEPOSIT')),
    CONSTRAINT ledger_entries_deposit_positive CHECK (type <> 'DEPOSIT' OR amount > 0)
);

-- Balance and history queries always filter by portfolio.
CREATE INDEX ledger_entries_portfolio_idx ON ledger_entries (portfolio_id, created_at);
