package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApplicationException {

    public InvalidDateRangeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
