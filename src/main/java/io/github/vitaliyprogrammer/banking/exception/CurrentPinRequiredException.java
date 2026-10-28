package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class CurrentPinRequiredException extends ApplicationException {

    public CurrentPinRequiredException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Current PIN is required to change the PIN");
    }
}
