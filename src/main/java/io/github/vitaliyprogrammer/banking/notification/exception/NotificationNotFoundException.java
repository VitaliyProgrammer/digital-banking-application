package io.github.vitaliyprogrammer.banking.notification.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Also for someone else's notification: its existence is not revealed. */
public class NotificationNotFoundException extends ApplicationException {

    public NotificationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Notification not found");
    }
}
