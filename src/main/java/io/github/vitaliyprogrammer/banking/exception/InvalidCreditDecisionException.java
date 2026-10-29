package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class InvalidCreditDecisionException extends ApplicationException {

    public InvalidCreditDecisionException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
