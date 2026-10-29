package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class CreditApplicationNotFoundException extends ApplicationException {

    public CreditApplicationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Credit application not found");
    }
}
