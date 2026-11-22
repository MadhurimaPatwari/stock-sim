package com.stocksim.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.stocksim.currency.CurrencyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Portfolios and their cash ledger. Every method that takes a userId checks the caller owns the portfolio. */
@Service
public class PortfolioService {

    /** Amounts are scaled to the currency's decimals (EUR 100.50, JPY 100). */
    public record PortfolioView(UUID id, String currency, BigDecimal cash, Instant createdAt) {}

    public record LedgerEntryView(UUID id, String type, BigDecimal amount, Instant createdAt) {}

    private final PortfolioRepository portfolios;
    private final LedgerEntryRepository ledger;
    private final CurrencyRepository currencies;

    public PortfolioService(PortfolioRepository portfolios, LedgerEntryRepository ledger, CurrencyRepository currencies) {
        this.portfolios = portfolios;
        this.ledger = ledger;
        this.currencies = currencies;
    }

    /** Called at signup, inside the signup transaction. */
    @Transactional
    public Portfolio open(UUID userId, String currency) {
        return portfolios.save(new Portfolio(userId, currency));
    }

    @Transactional(readOnly = true)
    public List<PortfolioView> listFor(UUID userId) {
        return portfolios.findByUserId(userId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public PortfolioView get(UUID portfolioId, UUID userId) {
        return view(owned(portfolioId, userId));
    }

    /**
     * Adds virtual cash. Amounts with more decimals than the currency allows are rejected, not rounded:
     * silently changing a money amount the user typed hides mistakes.
     *
     * @throws InvalidAmountException if the amount has too many decimal places for the currency
     */
    @Transactional
    public LedgerEntryView deposit(UUID portfolioId, UUID userId, BigDecimal amount) {
        Portfolio portfolio = owned(portfolioId, userId);
        int decimals = decimalsOf(portfolio);
        if (amount.stripTrailingZeros().scale() > decimals) {
            throw new InvalidAmountException(portfolio.getCurrency() + " allows at most " + decimals + " decimal places");
        }
        LedgerEntry entry = ledger.save(new LedgerEntry(portfolio.getId(), LedgerEntry.Type.DEPOSIT, amount.setScale(decimals)));
        return view(entry, decimals);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryView> ledger(UUID portfolioId, UUID userId) {
        Portfolio portfolio = owned(portfolioId, userId);
        int decimals = decimalsOf(portfolio);
        return ledger.findByPortfolioIdOrderByCreatedAtAsc(portfolio.getId()).stream()
                .map(e -> view(e, decimals))
                .toList();
    }

    private Portfolio owned(UUID portfolioId, UUID userId) {
        Portfolio portfolio = portfolios.findById(portfolioId).orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
        if (!portfolio.getUserId().equals(userId)) {
            throw new NotPortfolioOwnerException();
        }
        return portfolio;
    }

    private int decimalsOf(Portfolio portfolio) {
        return currencies.findById(portfolio.getCurrency()).orElseThrow().getMinorUnits();
    }

    private PortfolioView view(Portfolio portfolio) {
        BigDecimal cash = ledger.balanceOf(portfolio.getId()).setScale(decimalsOf(portfolio));
        return new PortfolioView(portfolio.getId(), portfolio.getCurrency(), cash, portfolio.getCreatedAt());
    }

    private static LedgerEntryView view(LedgerEntry entry, int decimals) {
        return new LedgerEntryView(entry.getId(), entry.getType().name(), entry.getAmount().setScale(decimals), entry.getCreatedAt());
    }
}
