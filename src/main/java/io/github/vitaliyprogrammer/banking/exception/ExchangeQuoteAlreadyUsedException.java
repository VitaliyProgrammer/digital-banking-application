package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class ExchangeQuoteAlreadyUsedException extends ApplicationException {

    public ExchangeQuoteAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "The quote has already been used");
    }
}
