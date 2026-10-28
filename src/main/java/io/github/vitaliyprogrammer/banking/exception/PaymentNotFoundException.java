package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApplicationException {

    public PaymentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Payment not found");
    }
}
