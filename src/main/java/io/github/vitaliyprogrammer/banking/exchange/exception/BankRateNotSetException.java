package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The bank has not priced this currency yet, so it cannot promise any rate. */
public class BankRateNotSetException extends ApplicationException {

    public BankRateNotSetException(String currency) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "bank-rate-not-set", "The bank rate for {0} is not set yet", currency);
    }
}
