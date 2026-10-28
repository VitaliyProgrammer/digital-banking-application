package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** Also used for someone else's account: 404, not 403, so the response does not confirm that it exists. */
public class AccountNotFoundException extends ApplicationException {

    public AccountNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Account not found");
    }
}
