package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class CurrentPinRequiredException extends ApplicationException {

    public CurrentPinRequiredException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Current PIN is required to change the PIN");
    }
}
