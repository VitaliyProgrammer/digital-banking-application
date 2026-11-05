package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The limit lives on the UNIVERSAL card, so the client applies for it once they have that card. */
public class UniversalCardRequiredException extends ApplicationException {

    public UniversalCardRequiredException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT,
                "A credit limit lives on your UNIVERSAL card: issue one on your UAH account first");
    }
}
