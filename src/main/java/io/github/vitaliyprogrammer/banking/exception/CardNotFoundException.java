package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** Also used for someone else's card: 404, not 403, so the response does not confirm that it exists. */
public class CardNotFoundException extends ApplicationException {

    public CardNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Card not found");
    }
}
