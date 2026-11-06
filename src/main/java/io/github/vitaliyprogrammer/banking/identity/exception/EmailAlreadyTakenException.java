package io.github.vitaliyprogrammer.banking.identity.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends ApplicationException {

    public EmailAlreadyTakenException() {
        super(HttpStatus.CONFLICT, "email-already-taken", "Email is already registered");
    }
}
