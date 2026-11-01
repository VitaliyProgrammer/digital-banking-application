package io.github.vitaliyprogrammer.banking.support.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Only the employee who took the ticket may answer it, resolve it or look at the client's accounts. */
public class NotTicketAssigneeException extends ApplicationException {

    public NotTicketAssigneeException() {
        super(HttpStatus.FORBIDDEN, "Only the employee who took this ticket may do this");
    }
}
