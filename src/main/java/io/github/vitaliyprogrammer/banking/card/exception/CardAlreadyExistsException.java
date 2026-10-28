package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.shared.ApplicationException;
import org.springframework.http.HttpStatus;

public class CardAlreadyExistsException extends ApplicationException {

    public CardAlreadyExistsException(CardType type) {
        super(HttpStatus.CONFLICT, "This account already has an open " + type + " card");
    }
}
