package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class RecipientNotFoundException extends ApplicationException {

    public RecipientNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Recipient account not found");
    }
}
