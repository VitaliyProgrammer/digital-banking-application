package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class TooManyLoginAttemptsException extends ApplicationException {

    public TooManyLoginAttemptsException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again in 15 minutes");
    }
}
