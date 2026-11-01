package io.github.vitaliyprogrammer.banking.exchange.dto.response;

import java.math.BigDecimal;

/**
 * Internal result of converting at the bank's rates, not an HTTP body.
 * {@code amount} is already rounded down to cents; {@code rate} is "target units per 1 source unit".
 */
public record ConversionDto(
        BigDecimal amount,
        BigDecimal rate
) {
}
