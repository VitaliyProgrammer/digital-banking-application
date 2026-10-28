package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class WeakPinException extends ApplicationException {

    public WeakPinException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "PIN is too easy to guess: avoid repeated or sequential digits");
    }
}
