package io.github.vitaliyprogrammer.banking.exception;

import io.github.vitaliyprogrammer.banking.entity.status.CardStatus;
import org.springframework.http.HttpStatus;

public class InvalidCardStateException extends ApplicationException {

    public InvalidCardStateException(CardStatus status) {
        super(HttpStatus.CONFLICT, "Not allowed while the card is " + status);
    }
}
