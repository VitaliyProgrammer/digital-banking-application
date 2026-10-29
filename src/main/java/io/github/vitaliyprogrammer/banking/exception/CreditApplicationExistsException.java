package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** One credit limit per client: a new application only after a decline. */
public class CreditApplicationExistsException extends ApplicationException {

    public CreditApplicationExistsException() {
        super(HttpStatus.CONFLICT, "You already have a credit limit or an application under review");
    }
}
