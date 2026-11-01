package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ExchangeQuoteNotFoundException extends ApplicationException {

    public ExchangeQuoteNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Exchange quote not found");
    }
}
