package com.stocksim.auth;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Authenticates "Authorization: Bearer ssk_..." API keys. Returns null for any other bearer token,
 * so the next provider (JWT) gets a turn.
 */
@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationProvider(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String token = ((BearerTokenAuthenticationToken) authentication).getToken();
        if (!token.startsWith(ApiKeyService.PREFIX)) {
            return null;
        }
        ApiKey apiKey = apiKeyService.authenticate(token)
                .orElseThrow(() -> new InvalidBearerTokenException("invalid or revoked API key"));

        // Same shape as a JWT login: the name is the user id, authorities are SCOPE_<scope>.
        List<SimpleGrantedAuthority> authorities = apiKey.getScopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        return UsernamePasswordAuthenticationToken.authenticated(apiKey.getUserId().toString(), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
