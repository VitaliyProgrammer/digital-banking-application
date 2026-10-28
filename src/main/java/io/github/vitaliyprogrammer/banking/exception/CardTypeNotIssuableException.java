package io.github.vitaliyprogrammer.banking.exception;

import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import org.springframework.http.HttpStatus;

public class CardTypeNotIssuableException extends ApplicationException {

    public CardTypeNotIssuableException(CardType type) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, type + " cards are issued only after a credit decision");
    }
}
