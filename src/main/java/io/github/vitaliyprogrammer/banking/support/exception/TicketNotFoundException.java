package io.github.vitaliyprogrammer.banking.support.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Also for someone else's ticket: its existence is not revealed. */
public class TicketNotFoundException extends ApplicationException {

    public TicketNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ticket-not-found", "Ticket not found");
    }
}
