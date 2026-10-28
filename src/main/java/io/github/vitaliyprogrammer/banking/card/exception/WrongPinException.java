package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class WrongPinException extends ApplicationException {

    public WrongPinException(long attemptsLeft) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Wrong PIN. Attempts left: " + attemptsLeft);
    }
}
