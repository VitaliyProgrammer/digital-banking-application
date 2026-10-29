package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class InvalidBankRateException extends ApplicationException {

    public InvalidBankRateException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
