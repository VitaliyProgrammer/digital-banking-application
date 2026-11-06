package io.github.vitaliyprogrammer.banking.identity.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** One answer for an unknown, used or expired link: which of them it was helps nobody but an attacker. */
public class InvalidPasswordResetTokenException extends ApplicationException {

    public InvalidPasswordResetTokenException() {
        super(HttpStatus.BAD_REQUEST, "invalid-password-reset-token", "This link is invalid or has expired. Ask for a new one");
    }
}
