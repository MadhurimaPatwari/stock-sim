package com.stocksim.portfolio;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(UUID id) {
        super("portfolio not found: " + id);
    }
}
