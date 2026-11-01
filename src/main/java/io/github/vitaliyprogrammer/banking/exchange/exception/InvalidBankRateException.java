package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidBankRateException extends ApplicationException {

    public InvalidBankRateException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
