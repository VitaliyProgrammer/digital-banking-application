package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApplicationException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
    }
}
