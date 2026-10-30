package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** Also for someone else's ticket: its existence is not revealed. */
public class TicketNotFoundException extends ApplicationException {

    public TicketNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Ticket not found");
    }
}
