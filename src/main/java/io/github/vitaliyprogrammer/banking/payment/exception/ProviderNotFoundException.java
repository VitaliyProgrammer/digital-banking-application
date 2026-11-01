package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ProviderNotFoundException extends ApplicationException {

    public ProviderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Provider not found");
    }
}
