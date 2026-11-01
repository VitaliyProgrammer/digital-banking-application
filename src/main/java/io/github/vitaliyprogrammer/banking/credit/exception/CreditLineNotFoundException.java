package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The client has no credit line: no application was approved yet. */
public class CreditLineNotFoundException extends ApplicationException {

    public CreditLineNotFoundException() {
        super(HttpStatus.NOT_FOUND, "You have no credit line");
    }
}
