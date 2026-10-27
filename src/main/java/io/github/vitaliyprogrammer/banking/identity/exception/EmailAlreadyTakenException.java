package io.github.vitaliyprogrammer.banking.identity.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends ApplicationException {

    public EmailAlreadyTakenException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
