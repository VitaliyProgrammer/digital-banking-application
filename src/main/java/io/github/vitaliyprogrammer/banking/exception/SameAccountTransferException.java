package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class SameAccountTransferException extends ApplicationException {

    public SameAccountTransferException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Cannot transfer money to the same account");
    }
}
