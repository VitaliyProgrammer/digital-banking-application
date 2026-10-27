package io.github.vitaliyprogrammer.banking.account.exception;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class AccountAlreadyExistsException extends ApplicationException {

    public AccountAlreadyExistsException(Currency currency) {
        super(HttpStatus.CONFLICT, "You already have an account in " + currency);
    }
}
