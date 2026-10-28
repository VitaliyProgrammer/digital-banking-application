package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class UserBlockedException extends ApplicationException {

    public UserBlockedException() {
        super(HttpStatus.FORBIDDEN, "User is blocked");
    }
}
