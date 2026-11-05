package io.github.vitaliyprogrammer.banking.tax.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The period is already paid in full, or there was no tax in it. */
public class NothingToPayException extends ApplicationException {

    public NothingToPayException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Nothing to pay for this period");
    }
}
