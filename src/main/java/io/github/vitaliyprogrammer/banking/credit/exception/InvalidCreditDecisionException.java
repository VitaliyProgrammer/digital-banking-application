package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidCreditDecisionException extends ApplicationException {

    /** Several cases share this class; each passes its own code, so a client can still tell them apart. */
    public InvalidCreditDecisionException(String code, String message, Object... args) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, code, message, args);
    }
}
