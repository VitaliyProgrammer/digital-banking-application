package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** UAH is the base currency: every rate is quoted against it, so it has no rate of its own. */
public class UnsupportedCurrencyException extends ApplicationException {

    public UnsupportedCurrencyException() {
        super(HttpStatus.BAD_REQUEST, "UAH is the base currency and has no exchange rate");
    }
}
