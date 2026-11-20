package com.ledgerlite.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
    }
}