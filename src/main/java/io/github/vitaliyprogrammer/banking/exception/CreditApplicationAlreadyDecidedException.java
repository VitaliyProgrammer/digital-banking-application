package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** Someone else decided first, or the rules already did: a decision is final. */
public class CreditApplicationAlreadyDecidedException extends ApplicationException {

    public CreditApplicationAlreadyDecidedException() {
        super(HttpStatus.CONFLICT, "The credit application has already been decided");
    }
}
