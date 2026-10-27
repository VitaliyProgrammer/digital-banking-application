package io.github.vitaliyprogrammer.banking.identity.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class UserBlockedException extends ApplicationException {

    public UserBlockedException() {
        super(HttpStatus.FORBIDDEN, "User is blocked");
    }
}
