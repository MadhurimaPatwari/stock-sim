package com.stocksim.auth;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Maps {@code api_keys} (V5). Holds only the hash of the key that was shown to the user. */
@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    /** Space-separated, e.g. "portfolio:deposit portfolio:read". */
    @Column(nullable = false, length = 200)
    private String scopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ApiKey() {
        // for JPA
    }

    public ApiKey(UUID userId, String name, String keyHash, Set<String> scopes) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.keyHash = keyHash;
        this.scopes = String.join(" ", new TreeSet<>(scopes));
        this.createdAt = Instant.now();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Idempotent: revoking twice keeps the first revocation time. */
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public void markUsed(Instant now) {
        lastUsedAt = now;
    }

    public Set<String> getScopes() {
        return new TreeSet<>(Arrays.asList(scopes.split(" ")));
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
