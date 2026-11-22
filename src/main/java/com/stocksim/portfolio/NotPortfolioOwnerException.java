package com.stocksim.portfolio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotPortfolioOwnerException extends RuntimeException {

    public NotPortfolioOwnerException() {
        super("you can only access your own portfolios");
    }
}
