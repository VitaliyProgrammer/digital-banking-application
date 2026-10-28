package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApplicationException {

    public AccountNotActiveException(String accountNumber) {
        super(HttpStatus.CONFLICT, "Account " + accountNumber + " is not active");
    }
}
