package io.github.vitaliyprogrammer.banking.account.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** One credit limit per client: a second approval must not silently replace the first. */
public class CreditLineAlreadyExistsException extends ApplicationException {

    public CreditLineAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "This account already has a credit limit");
    }
}
