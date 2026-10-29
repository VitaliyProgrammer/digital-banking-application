package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class ExchangeQuoteNotFoundException extends ApplicationException {

    public ExchangeQuoteNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Exchange quote not found");
    }
}
