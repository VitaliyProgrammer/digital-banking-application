package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** Same message for unknown email and wrong password: login must not reveal who is a client. */
public class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
