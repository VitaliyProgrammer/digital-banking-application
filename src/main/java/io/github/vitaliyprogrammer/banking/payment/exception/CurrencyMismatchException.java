package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

/** Currency exchange is not supported yet, so both accounts of a transfer must share a currency. */
public class CurrencyMismatchException extends ApplicationException {

    public CurrencyMismatchException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Both accounts must be in the same currency");
    }
}
