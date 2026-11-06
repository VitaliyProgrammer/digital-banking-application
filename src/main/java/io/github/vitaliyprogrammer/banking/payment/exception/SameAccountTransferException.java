package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class SameAccountTransferException extends ApplicationException {

    public SameAccountTransferException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "same-account-transfer", "Cannot transfer money to the same account");
    }
}
