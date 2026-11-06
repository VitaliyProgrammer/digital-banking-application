package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class WeakPinException extends ApplicationException {

    public WeakPinException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "weak-pin", "PIN is too easy to guess: avoid repeated or sequential digits");
    }
}
