package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** The NBU did not answer, or no official rate has been loaded yet. */
public class OfficialRatesUnavailableException extends ApplicationException {

    public OfficialRatesUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
