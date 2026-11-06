package io.github.vitaliyprogrammer.banking.account.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApplicationException {

    public AccountNotActiveException(String accountNumber) {
        super(HttpStatus.CONFLICT, "account-not-active", "Account {0} is not active", accountNumber);
    }
}
