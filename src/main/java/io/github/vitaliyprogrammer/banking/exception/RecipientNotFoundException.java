package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class RecipientNotFoundException extends ApplicationException {

    public RecipientNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Recipient account not found");
    }
}
