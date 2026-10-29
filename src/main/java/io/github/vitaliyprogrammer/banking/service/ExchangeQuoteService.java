package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.ExchangeQuoteRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.ExchangeQuoteResponseDto;
import java.util.UUID;

public interface ExchangeQuoteService {

    /** Prices the exchange at the bank's current rates and holds that price for a short time. */
    ExchangeQuoteResponseDto createQuote(UUID userId, ExchangeQuoteRequestDto request);

    /**
     * For the payment that spends the quote: locks it until the caller's transaction ends, so two requests
     * cannot spend it at the same time. Must be called inside that transaction.
     */
    ExchangeQuoteResponseDto lockForUse(UUID userId, UUID quoteId);
}
