package io.github.vitaliyprogrammer.banking.business.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** One sole proprietor per client, and one per tax number. */
public class BusinessAlreadyExistsException extends ApplicationException {

    public BusinessAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "A sole proprietor is already registered for this client or tax number");
    }
}
