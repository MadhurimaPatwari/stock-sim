package com.stocksim.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.stocksim.portfolio.PortfolioService.LedgerEntryView;
import com.stocksim.portfolio.PortfolioService.PortfolioView;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The logged-in user's portfolios. Ownership is checked in {@link PortfolioService}.
 *
 * GET  /api/portfolios                  my portfolios with cash balance
 * GET  /api/portfolios/{id}             one portfolio
 * POST /api/portfolios/{id}/deposits    add virtual cash
 * POST /api/portfolios/{id}/withdrawals take cash out (409 if the balance is too low)
 * GET  /api/portfolios/{id}/ledger      every cash movement, oldest first
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    public record DepositRequest(@NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {}

    /** The amount to take out, as a positive number; it is stored as a negative ledger entry. */
    public record WithdrawalRequest(@NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {}

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioView> mine(Authentication auth) {
        return portfolioService.listFor(currentUserId(auth));
    }

    @GetMapping("/{id}")
    public PortfolioView get(@PathVariable UUID id, Authentication auth) {
        return portfolioService.get(id, currentUserId(auth));
    }

    @PostMapping("/{id}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryView deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest req,
                                   Authentication auth) {
        return portfolioService.deposit(id, currentUserId(auth), req.amount());
    }

    @PostMapping("/{id}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryView withdraw(@PathVariable UUID id, @Valid @RequestBody WithdrawalRequest req,
                                    Authentication auth) {
        return portfolioService.withdraw(id, currentUserId(auth), req.amount());
    }

    @GetMapping("/{id}/ledger")
    public List<LedgerEntryView> ledger(@PathVariable UUID id, Authentication auth) {
        return portfolioService.ledger(id, currentUserId(auth));
    }

    private static UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());   // the user id, for login JWTs and API keys alike
    }
}
