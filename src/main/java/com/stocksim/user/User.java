package com.stocksim.user;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Maps the {@code users} table created in V1__create_users.sql. */
@Entity
@Table(name = "users")
public class User {

    public enum Status { ACTIVE, SUSPENDED, CLOSED }

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    // Column is CHAR(3), not VARCHAR; tell Hibernate so schema validation passes.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reporting_currency", nullable = false, length = 3)
    private String reportingCurrency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Version
    private long version;

    protected User() {
        // for JPA
    }

    public User(String email, String passwordHash, String displayName, String reportingCurrency) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.reportingCurrency = reportingCurrency;
        this.status = Status.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /**
     * Counts a wrong password. On the {@code maxAttempts}-th consecutive failure the account is locked
     * until {@code lockUntil} and the counter starts over.
     *
     * @return true if this failure locked the account
     */
    public boolean recordFailedLogin(int maxAttempts, Instant lockUntil) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = lockUntil;
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
    }

    public void changePassword(String newPasswordHash) {
        passwordHash = newPasswordHash;
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public Status getStatus() { return status; }
    public String getReportingCurrency() { return reportingCurrency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
