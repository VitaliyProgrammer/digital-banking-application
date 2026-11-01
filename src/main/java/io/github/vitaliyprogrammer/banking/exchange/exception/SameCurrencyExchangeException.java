package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class SameCurrencyExchangeException extends ApplicationException {

    public SameCurrencyExchangeException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Exchange needs two accounts in different currencies");
    }
}
