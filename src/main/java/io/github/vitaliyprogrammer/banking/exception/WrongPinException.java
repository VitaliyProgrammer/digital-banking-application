package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class WrongPinException extends ApplicationException {

    public WrongPinException(long attemptsLeft) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Wrong PIN. Attempts left: " + attemptsLeft);
    }
}
