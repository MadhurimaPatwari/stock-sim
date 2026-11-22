package com.stocksim.portfolio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Maps {@code ledger_entries} (V4). Append-only: created once, never updated or deleted. */
@Entity
@Immutable
@Table(name = "ledger_entries")
public class LedgerEntry {

    public enum Type { DEPOSIT }

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    /** Positive adds cash, negative removes it. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
        // for JPA
    }

    public LedgerEntry(UUID portfolioId, Type type, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.portfolioId = portfolioId;
        this.type = type;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
