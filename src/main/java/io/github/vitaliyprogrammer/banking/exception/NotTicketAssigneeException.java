package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** Only the employee who took the ticket may answer it, resolve it or look at the client's accounts. */
public class NotTicketAssigneeException extends ApplicationException {

    public NotTicketAssigneeException() {
        super(HttpStatus.FORBIDDEN, "Only the employee who took this ticket may do this");
    }
}
