package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class ReceiptEmailFailedException extends ApplicationException {

    public ReceiptEmailFailedException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The receipt could not be sent right now. Please try again later");
    }
}
