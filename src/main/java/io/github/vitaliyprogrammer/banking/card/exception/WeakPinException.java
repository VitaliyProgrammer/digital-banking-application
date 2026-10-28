package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class WeakPinException extends ApplicationException {

    public WeakPinException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "PIN is too easy to guess: avoid repeated or sequential digits");
    }
}
