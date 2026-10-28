package io.github.vitaliyprogrammer.banking.exception;

import org.springframework.http.HttpStatus;

/** 422, as in the IETF "Idempotency-Key HTTP Header Field" draft: the key belongs to a different request. */
public class IdempotencyKeyReusedException extends ApplicationException {

    public IdempotencyKeyReusedException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Idempotency-Key was already used with a different request");
    }
}
