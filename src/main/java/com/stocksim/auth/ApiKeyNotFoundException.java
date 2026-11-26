package com.stocksim.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(UUID id) {
        super("API key not found: " + id);
    }
}
