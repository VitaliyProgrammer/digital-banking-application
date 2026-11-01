package io.github.vitaliyprogrammer.banking.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. Every module extends it, and the global handler
 * turns it into an RFC 9457 problem response with the given status.
 */
public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;

    protected ApplicationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
