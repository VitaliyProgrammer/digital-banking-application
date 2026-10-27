package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class SameAccountTransferException extends ApplicationException {

    public SameAccountTransferException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Cannot transfer money to the same account");
    }
}
