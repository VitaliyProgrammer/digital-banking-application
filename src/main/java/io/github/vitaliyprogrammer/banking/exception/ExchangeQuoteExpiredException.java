package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** The promised price is gone: the client must ask for a new quote and see the new price before agreeing. */
public class ExchangeQuoteExpiredException extends ApplicationException {

    public ExchangeQuoteExpiredException() {
        super(HttpStatus.CONFLICT, "The quote has expired. Request a new one");
    }
}
