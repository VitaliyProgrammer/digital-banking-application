package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Business money and personal money stay apart: a card goes only on the matching kind of account. */
public class CardTypeNotForAccountException extends ApplicationException {

    public CardTypeNotForAccountException(CardType type) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, type == CardType.BUSINESS
                ? "A BUSINESS card goes on a sole proprietor's business account"
                : "A " + type + " card goes on a personal current account");
    }
}
