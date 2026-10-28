package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "User not found");
    }
}
