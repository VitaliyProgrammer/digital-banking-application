package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** 423 Locked: the card exists and the request is valid, but the card itself is locked. */
public class CardPinLockedException extends ApplicationException {

    public CardPinLockedException() {
        super(HttpStatus.LOCKED, "Card is locked after too many wrong PIN attempts. Contact support");
    }
}
