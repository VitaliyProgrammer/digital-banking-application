package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class PinNotSetException extends ApplicationException {

    public PinNotSetException() {
        super(HttpStatus.CONFLICT, "PIN is not set for this card yet");
    }
}
