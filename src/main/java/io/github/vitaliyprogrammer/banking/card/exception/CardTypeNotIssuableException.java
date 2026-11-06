package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class CardTypeNotIssuableException extends ApplicationException {

    public CardTypeNotIssuableException(CardType type) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "card-type-not-issuable", "{0} cards are issued only after a credit decision",
                type);
    }
}
