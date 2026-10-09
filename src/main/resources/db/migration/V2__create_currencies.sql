-- Currencies: reference (lookup) table of ISO 4217 codes the platform supports.
CREATE TABLE currencies (
    code         CHAR(3)     PRIMARY KEY,
    name         VARCHAR(50) NOT NULL,
    minor_units  SMALLINT    NOT NULL,

    CONSTRAINT currencies_code_format  CHECK (code ~ '^[A-Z]{3}$'),
    CONSTRAINT currencies_minor_units  CHECK (minor_units BETWEEN 0 AND 4)
);

INSERT INTO currencies (code, name, minor_units) VALUES
    ('USD', 'US Dollar',         2),
    ('EUR', 'Euro',              2),
    ('INR', 'Indian Rupee',      2);

-- From now on a user's currency must exist in the table above.
ALTER TABLE users
    ADD CONSTRAINT users_reporting_currency_fk
    FOREIGN KEY (reporting_currency) REFERENCES currencies (code);
