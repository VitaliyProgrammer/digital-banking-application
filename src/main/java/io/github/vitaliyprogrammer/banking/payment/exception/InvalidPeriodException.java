package io.github.vitaliyprogrammer.banking.payment.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** A period that ends before it starts, or longer than a year. */
public class InvalidPeriodException extends ApplicationException {

    public InvalidPeriodException(long maxDays) {
        super(HttpStatus.BAD_REQUEST, "invalid-period",
                "The period must start before it ends and be at most {0} days long", maxDays);
    }
}
