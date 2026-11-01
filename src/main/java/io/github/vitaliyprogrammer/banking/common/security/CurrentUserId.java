package io.github.vitaliyprogrammer.banking.common.security;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Injects the id of the authenticated user, taken from the verified JWT "sub" claim:
 * {@code public AccountResponseDto open(@CurrentUserId UUID userId, ...)}.
 * Hidden from Swagger: the client never sends it, it always comes from the token.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
@AuthenticationPrincipal(expression = "T(java.util.UUID).fromString(subject)")
public @interface CurrentUserId {
}
