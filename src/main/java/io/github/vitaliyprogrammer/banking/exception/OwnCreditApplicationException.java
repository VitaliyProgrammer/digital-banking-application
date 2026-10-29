package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** The four-eyes rule: a bank employee never decides on their own credit. */
public class OwnCreditApplicationException extends ApplicationException {

    public OwnCreditApplicationException() {
        super(HttpStatus.FORBIDDEN, "You cannot decide on your own credit application");
    }
}
