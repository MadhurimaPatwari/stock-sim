package com.stocksim.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Scoped to the owner: someone else's key looks exactly like a key that doesn't exist. */
    Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);
}
