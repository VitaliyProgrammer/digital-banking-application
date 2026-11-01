package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The card exists but cannot move money right now: blocked, locked, closed or expired. */
public class CardNotUsableException extends ApplicationException {

    public CardNotUsableException(String whichCard) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, whichCard + " cannot be used for payments");
    }
}
