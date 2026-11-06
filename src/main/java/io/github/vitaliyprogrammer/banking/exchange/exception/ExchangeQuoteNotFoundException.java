package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ExchangeQuoteNotFoundException extends ApplicationException {

    public ExchangeQuoteNotFoundException() {
        super(HttpStatus.NOT_FOUND, "exchange-quote-not-found", "Exchange quote not found");
    }
}
