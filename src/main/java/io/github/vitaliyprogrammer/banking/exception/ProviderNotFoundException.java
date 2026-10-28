package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class ProviderNotFoundException extends ApplicationException {

    public ProviderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Provider not found");
    }
}
