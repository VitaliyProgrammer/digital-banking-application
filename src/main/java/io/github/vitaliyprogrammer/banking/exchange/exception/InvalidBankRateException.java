package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidBankRateException extends ApplicationException {

    /** Several cases share this class; each passes its own code, so a client can still tell them apart. */
    public InvalidBankRateException(String code, String message, Object... args) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, code, message, args);
    }
}
