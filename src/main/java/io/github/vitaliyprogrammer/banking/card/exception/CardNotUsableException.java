package io.github.vitaliyprogrammer.banking.card.exception;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** The card exists but cannot move money right now: blocked, locked, closed or expired. */
public class CardNotUsableException extends ApplicationException {

    private CardNotUsableException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    /** The client's own card: they can unblock it or use another one. */
    public static CardNotUsableException ownCard() {
        return new CardNotUsableException("card-not-usable", "Your card cannot be used for payments");
    }

    /** The recipient's card: the client can only ask for another number. */
    public static CardNotUsableException recipientCard() {
        return new CardNotUsableException("recipient-card-not-usable", "Recipient's card cannot be used for payments");
    }
}
