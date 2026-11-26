package com.stocksim.auth;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Personal API keys. Needs the api-keys:manage scope, which only login tokens have (see SecurityConfig).
 *
 * POST   /api/api-keys       create a key; the key itself is in this response only
 * GET    /api/api-keys       my keys (never the key itself)
 * DELETE /api/api-keys/{id}  revoke a key
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    public record CreateApiKeyRequest(@NotBlank @Size(max = 100) String name, @NotEmpty Set<String> scopes) {}

    public record ApiKeyResponse(UUID id, String name, Set<String> scopes, Instant createdAt,
                                 Instant lastUsedAt, Instant revokedAt) {
        static ApiKeyResponse from(ApiKey k) {
            return new ApiKeyResponse(k.getId(), k.getName(), k.getScopes(), k.getCreatedAt(),
                    k.getLastUsedAt(), k.getRevokedAt());
        }
    }

    /** Returned once, at creation. Store the key now: it can't be shown again. */
    public record CreatedApiKeyResponse(UUID id, String name, Set<String> scopes, String key) {}

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedApiKeyResponse create(@Valid @RequestBody CreateApiKeyRequest req, Authentication auth) {
        ApiKeyService.CreatedKey created = apiKeyService.create(currentUserId(auth), req.name(), req.scopes());
        ApiKey k = created.apiKey();
        return new CreatedApiKeyResponse(k.getId(), k.getName(), k.getScopes(), created.plainKey());
    }

    @GetMapping
    public List<ApiKeyResponse> list(Authentication auth) {
        return apiKeyService.list(currentUserId(auth)).stream().map(ApiKeyResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id, Authentication auth) {
        apiKeyService.revoke(id, currentUserId(auth));
    }

    private static UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
