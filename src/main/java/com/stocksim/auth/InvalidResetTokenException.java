package com.stocksim.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("invalid or expired reset token");
    }
}
