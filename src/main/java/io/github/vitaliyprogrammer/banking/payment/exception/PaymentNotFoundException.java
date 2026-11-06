package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApplicationException {

    public PaymentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "payment-not-found", "Payment not found");
    }
}
