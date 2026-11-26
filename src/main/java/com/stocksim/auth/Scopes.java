package com.stocksim.auth;

import java.util.Set;

/**
 * What a token may do. Login tokens get {@link #LOGIN}; an API key gets the subset chosen when it was created.
 * Spring turns each scope into an authority "SCOPE_<scope>", which SecurityConfig checks per route.
 */
public final class Scopes {

    public static final String PORTFOLIO_READ = "portfolio:read";
    public static final String PORTFOLIO_DEPOSIT = "portfolio:deposit";
    /** Create, list and revoke API keys. Never grantable to an API key, so a key can't mint more keys. */
    public static final String API_KEYS_MANAGE = "api-keys:manage";

    public static final Set<String> LOGIN = Set.of(PORTFOLIO_READ, PORTFOLIO_DEPOSIT, API_KEYS_MANAGE);
    public static final Set<String> GRANTABLE_TO_API_KEYS = Set.of(PORTFOLIO_READ, PORTFOLIO_DEPOSIT);

    private Scopes() {
    }
}
