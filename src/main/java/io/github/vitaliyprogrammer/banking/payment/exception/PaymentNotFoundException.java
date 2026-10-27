package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApplicationException {

    public PaymentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Payment not found");
    }
}
