package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidCardStateException extends ApplicationException {

    /** One code per status: "the card is blocked" and "the card is closed" tell the client different things to do. */
    public InvalidCardStateException(CardStatus status) {
        super(HttpStatus.CONFLICT, codeOf(status), "Not allowed while the card is {0}", status);
    }

    private static String codeOf(CardStatus status) {
        return switch (status) {
            case ACTIVE -> "card-is-active";
            case BLOCKED -> "card-is-blocked";
            case PIN_LOCKED -> "card-is-pin-locked";
            case CLOSED -> "card-is-closed";
        };
    }
}
