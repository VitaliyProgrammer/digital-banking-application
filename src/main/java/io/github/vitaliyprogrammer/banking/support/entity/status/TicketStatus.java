package io.github.vitaliyprogrammer.banking.support.entity.status;

public enum TicketStatus {

    /** In the support queue, nobody owns it yet. */
    OPEN,

    /** Taken by one employee, who alone may answer and resolve it. */
    IN_PROGRESS,

    /** Final: a new question is a new ticket. */
    RESOLVED
}
