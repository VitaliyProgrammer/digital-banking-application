package io.github.vitaliyprogrammer.banking.account.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

/** 422: the request is valid, but the current state of the account does not allow it. */
public class InsufficientFundsException extends ApplicationException {

    public InsufficientFundsException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds");
    }
}
