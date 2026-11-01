package io.github.vitaliyprogrammer.banking.exchange.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** E.g. 0.01 UAH is less than one cent: the client would give money and get nothing back. */
public class ExchangeAmountTooSmallException extends ApplicationException {

    public ExchangeAmountTooSmallException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "The amount is too small to exchange");
    }
}
