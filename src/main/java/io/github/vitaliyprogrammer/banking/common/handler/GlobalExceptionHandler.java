package io.github.vitaliyprogrammer.banking.common.handler;

import io.github.vitaliyprogrammer.banking.common.exception.ApplicationException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * One error format for the whole API, in the client's language (Accept-Language; English by default).
 * Spring MVC's own exceptions (bad JSON, wrong method, 404) are handled by {@link ResponseEntityExceptionHandler},
 * which finds their texts in the same message bundles; this class adds business errors, field-level validation
 * details and a safe fallback for everything unexpected.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    /**
     * {@code detail} is for people and may change; {@code code} is for programs and never does. A client that needs
     * to react to an error (a mobile app showing its own screen for "insufficient-funds") branches on the code.
     */
    @ExceptionHandler(ApplicationException.class)
    ProblemDetail handleApplicationException(ApplicationException ex, Locale locale) {
        String detail = messageSource.getMessage("error." + ex.getCode(), ex.getArgs(), ex.getMessage(), locale);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), detail);
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    /** One message per field, already in the client's language: the validator interpolates with the request locale. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // The default message is the finished text. Resolving it again through the MessageSource would run it
            // through MessageFormat, which eats apostrophes and quotes.
            errors.putIfAbsent(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid"));
        }

        ProblemDetail problem = ex.getBody();
        problem.setDetail(messageSource.getMessage("request.validation-failed", null, locale));
        problem.setProperty("code", "validation-failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Never leak stack traces or internal messages to the client; log them instead. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, Locale locale) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                messageSource.getMessage("error.unexpected", null, locale));
        problem.setProperty("code", "unexpected");
        return problem;
    }
}
