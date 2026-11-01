package io.github.vitaliyprogrammer.banking.receipt.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** A receipt confirms that money moved; a payment still in progress or failed has nothing to confirm. */
public class ReceiptNotAvailableException extends ApplicationException {

    public ReceiptNotAvailableException() {
        super(HttpStatus.CONFLICT, "A receipt is available only for a completed payment");
    }
}
