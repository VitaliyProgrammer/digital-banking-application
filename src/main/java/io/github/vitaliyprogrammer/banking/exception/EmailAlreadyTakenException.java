package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends ApplicationException {

    public EmailAlreadyTakenException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
