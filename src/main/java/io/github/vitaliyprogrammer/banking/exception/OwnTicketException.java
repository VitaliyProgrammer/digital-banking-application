package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** An employee who is also a client never handles their own ticket. */
public class OwnTicketException extends ApplicationException {

    public OwnTicketException() {
        super(HttpStatus.FORBIDDEN, "You cannot handle your own ticket");
    }
}
