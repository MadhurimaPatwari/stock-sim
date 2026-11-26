package com.stocksim.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidScopeException extends RuntimeException {

    public InvalidScopeException(String scope) {
        super("scope '" + scope + "' can't be given to an API key; allowed: " + Scopes.GRANTABLE_TO_API_KEYS);
    }
}
