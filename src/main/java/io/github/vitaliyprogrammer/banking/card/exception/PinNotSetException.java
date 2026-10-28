package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class PinNotSetException extends ApplicationException {

    public PinNotSetException() {
        super(HttpStatus.CONFLICT, "PIN is not set for this card yet");
    }
}
