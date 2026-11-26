package com.stocksim.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Personal API keys: long-lived, scoped, revocable credentials for programs acting as a user. */
@Service
public class ApiKeyService {

    /** Marks a bearer token as an API key rather than a JWT, and lets secret scanners recognise leaked keys. */
    public static final String PREFIX = "ssk_";

    /** The key in plain text exists only in this return value; we keep just its hash. */
    public record CreatedKey(ApiKey apiKey, String plainKey) {}

    private final ApiKeyRepository keys;

    public ApiKeyService(ApiKeyRepository keys) {
        this.keys = keys;
    }

    /** @throws InvalidScopeException if a scope can't be given to an API key */
    @Transactional
    public CreatedKey create(UUID userId, String name, Set<String> scopes) {
        for (String scope : scopes) {
            if (!Scopes.GRANTABLE_TO_API_KEYS.contains(scope)) {
                throw new InvalidScopeException(scope);
            }
        }
        String plainKey = PREFIX + Tokens.newSecret();
        ApiKey apiKey = keys.save(new ApiKey(userId, name, Tokens.sha256(plainKey), scopes));
        return new CreatedKey(apiKey, plainKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> list(UUID userId) {
        return keys.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /** @throws ApiKeyNotFoundException if the key doesn't exist or belongs to someone else */
    @Transactional
    public void revoke(UUID keyId, UUID userId) {
        keys.findByIdAndUserId(keyId, userId)
                .orElseThrow(() -> new ApiKeyNotFoundException(keyId))
                .revoke(Instant.now());
    }

    /** Looks up a presented key. Empty if unknown or revoked. Records when it was last used. */
    @Transactional
    public Optional<ApiKey> authenticate(String plainKey) {
        Optional<ApiKey> apiKey = keys.findByKeyHash(Tokens.sha256(plainKey)).filter(k -> !k.isRevoked());
        apiKey.ifPresent(k -> k.markUsed(Instant.now()));
        return apiKey;
    }
}
