package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ExchangeQuoteAlreadyUsedException extends ApplicationException {

    public ExchangeQuoteAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "exchange-quote-already-used", "The quote has already been used");
    }
}
