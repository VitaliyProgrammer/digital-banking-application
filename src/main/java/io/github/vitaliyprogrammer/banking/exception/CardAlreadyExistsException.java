package io.github.vitaliyprogrammer.banking.exception;

import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import org.springframework.http.HttpStatus;

public class CardAlreadyExistsException extends ApplicationException {

    public CardAlreadyExistsException(CardType type) {
        super(HttpStatus.CONFLICT, "This account already has an open " + type + " card");
    }
}
