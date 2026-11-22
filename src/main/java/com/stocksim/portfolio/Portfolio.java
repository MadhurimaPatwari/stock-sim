package com.stocksim.portfolio;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Maps {@code portfolios} (V4). Holds no balance column: cash is the sum of its ledger entries. */
@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Portfolio() {
        // for JPA
    }

    public Portfolio(UUID userId, String currency) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
