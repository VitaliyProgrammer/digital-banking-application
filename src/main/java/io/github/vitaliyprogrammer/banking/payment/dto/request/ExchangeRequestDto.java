package io.github.vitaliyprogrammer.banking.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Only the quote: accounts, amounts and the rate were fixed when the client saw the price. */
public record ExchangeRequestDto(
        @NotNull
        UUID quoteId
) {
}
