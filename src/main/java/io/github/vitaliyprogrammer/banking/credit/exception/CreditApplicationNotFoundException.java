package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class CreditApplicationNotFoundException extends ApplicationException {

    public CreditApplicationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Credit application not found");
    }
}
