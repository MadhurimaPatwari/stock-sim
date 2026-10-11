package com.stocksim.currency;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String code) {
        super("unsupported currency '" + code + "'; see GET /api/currencies");
    }
}
