package io.github.vitaliyprogrammer.banking.common.exception;

import java.text.MessageFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors. Every module extends it, and the global handler turns it into an
 * RFC 9457 problem response with the given status, a stable {@code code} and the text in the client's language.
 *
 * <p>The English text lives here, next to the reason for the error, and is what the log shows. Other languages
 * translate it by code: {@code error.<code>} in {@code messages_uk.properties}.
 */
public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;

    private final String code;

    /** Not part of serialization: a business error never leaves the JVM as a Java object. */
    private final transient Object[] args;

    /**
     * @param code    machine-readable and stable, e.g. {@code insufficient-funds}: a client may branch on it, so it
     *                never changes, while the text may
     * @param message the English text; with {@code args} a {@link MessageFormat} pattern, e.g. "Attempts left: {0}"
     */
    protected ApplicationException(HttpStatus status, String code, String message, Object... args) {
        super(args.length == 0 ? message : new MessageFormat(message, Locale.ENGLISH).format(args));
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
