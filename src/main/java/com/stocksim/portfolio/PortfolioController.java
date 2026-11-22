package com.stocksim.portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.stocksim.portfolio.PortfolioService.LedgerEntryView;
import com.stocksim.portfolio.PortfolioService.PortfolioView;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * GET  /api/portfolios/{id}/ledger      every cash movement, oldest first
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    public record DepositRequest(@NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount) {}

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioView> mine(@AuthenticationPrincipal Jwt jwt) {
        return portfolioService.listFor(currentUserId(jwt));
    }

    @GetMapping("/{id}")
    public PortfolioView get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return portfolioService.get(id, currentUserId(jwt));
    }

    @PostMapping("/{id}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryView deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest req,
                                   @AuthenticationPrincipal Jwt jwt) {
        return portfolioService.deposit(id, currentUserId(jwt), req.amount());
    }

    @GetMapping("/{id}/ledger")
    public List<LedgerEntryView> ledger(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return portfolioService.ledger(id, currentUserId(jwt));
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
