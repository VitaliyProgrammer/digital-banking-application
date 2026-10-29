package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApplicationException {

    public InvalidDateRangeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
