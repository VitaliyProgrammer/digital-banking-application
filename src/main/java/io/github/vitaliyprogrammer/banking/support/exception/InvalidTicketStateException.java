package io.github.vitaliyprogrammer.banking.support.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** 409: the ticket has moved on, e.g. another employee took it first, or it is resolved. */
public class InvalidTicketStateException extends ApplicationException {

    /** Several cases share this class; each passes its own code, so a client can still tell them apart. */
    public InvalidTicketStateException(String code, String message, Object... args) {
        super(HttpStatus.CONFLICT, code, message, args);
    }
}
