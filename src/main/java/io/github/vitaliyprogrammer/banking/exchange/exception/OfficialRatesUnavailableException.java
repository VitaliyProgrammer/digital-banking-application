package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The NBU did not answer, or no official rate has been loaded yet. */
public class OfficialRatesUnavailableException extends ApplicationException {

    /** Several cases share this class; each passes its own code, so a client can still tell them apart. */
    public OfficialRatesUnavailableException(String code, String message, Object... args) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, message, args);
    }
}
