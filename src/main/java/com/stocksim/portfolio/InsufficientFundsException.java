package com.stocksim.portfolio;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 409: the request is valid, but conflicts with the portfolio's current state (its balance). */
@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String currency, BigDecimal balance, BigDecimal requested) {
        super("insufficient funds: balance is " + currency + " " + balance + ", requested " + currency + " " + requested);
    }
}
