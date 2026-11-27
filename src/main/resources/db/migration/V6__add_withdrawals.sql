-- Withdrawals: money leaving a portfolio, stored as a negative amount so the balance stays SUM(amount).
-- "Never below zero" depends on the sum of many rows, which a CHECK can't express; PortfolioService
-- enforces it while holding a lock on the portfolio row.
ALTER TABLE ledger_entries DROP CONSTRAINT ledger_entries_type_valid;
ALTER TABLE ledger_entries
    ADD CONSTRAINT ledger_entries_type_valid          CHECK (type IN ('DEPOSIT', 'WITHDRAWAL')),
    ADD CONSTRAINT ledger_entries_withdrawal_negative CHECK (type <> 'WITHDRAWAL' OR amount < 0);
