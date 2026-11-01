package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidCardStateException extends ApplicationException {

    public InvalidCardStateException(CardStatus status) {
        super(HttpStatus.CONFLICT, "Not allowed while the card is " + status);
    }
}
