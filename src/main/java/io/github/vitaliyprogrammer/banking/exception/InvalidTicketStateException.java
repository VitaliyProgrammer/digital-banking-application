package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** 409: the ticket has moved on, e.g. another employee took it first, or it is resolved. */
public class InvalidTicketStateException extends ApplicationException {

    public InvalidTicketStateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
