package io.github.vitaliyprogrammer.banking.exception;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import org.springframework.http.HttpStatus;

public class AccountAlreadyExistsException extends ApplicationException {

    public AccountAlreadyExistsException(Currency currency) {
        super(HttpStatus.CONFLICT, "You already have an account in " + currency);
    }
}
