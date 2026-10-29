package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** The bank has not priced this currency yet, so it cannot promise any rate. */
public class BankRateNotSetException extends ApplicationException {

    public BankRateNotSetException(String currency) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The bank rate for " + currency + " is not set yet");
    }
}
