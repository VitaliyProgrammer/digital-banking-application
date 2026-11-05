package io.github.vitaliyprogrammer.banking.business.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The client has not opened a sole proprietor's account. */
public class BusinessNotFoundException extends ApplicationException {

    public BusinessNotFoundException() {
        super(HttpStatus.NOT_FOUND, "You have no sole proprietor's account yet");
    }
}
