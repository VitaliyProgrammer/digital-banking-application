package io.github.vitaliyprogrammer.banking.credit.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Scoring reads the income of the client's UAH current account, so there must be one. */
public class CurrentAccountRequiredException extends ApplicationException {

    public CurrentAccountRequiredException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Open a UAH current account before applying for a credit limit");
    }
}
