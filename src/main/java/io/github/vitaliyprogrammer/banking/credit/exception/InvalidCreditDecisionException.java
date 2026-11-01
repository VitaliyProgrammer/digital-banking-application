package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidCreditDecisionException extends ApplicationException {

    public InvalidCreditDecisionException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
