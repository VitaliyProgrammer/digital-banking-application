package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.configuration.ExchangeRateProperties;
import io.github.vitaliyprogrammer.banking.dto.request.ExchangeQuoteRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.ConversionDto;
import io.github.vitaliyprogrammer.banking.dto.response.ExchangeQuoteResponseDto;
import io.github.vitaliyprogrammer.banking.entity.ExchangeQuote;
import io.github.vitaliyprogrammer.banking.exception.ExchangeAmountTooSmallException;
import io.github.vitaliyprogrammer.banking.exception.ExchangeQuoteNotFoundException;
import io.github.vitaliyprogrammer.banking.exception.SameCurrencyExchangeException;
import io.github.vitaliyprogrammer.banking.repository.ExchangeQuoteRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.ExchangeQuoteService;
import io.github.vitaliyprogrammer.banking.service.ExchangeRateService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeQuoteServiceImpl implements ExchangeQuoteService {

    private final ExchangeQuoteRepository exchangeQuoteRepository;

    private final AccountService accountService;

    private final ExchangeRateService exchangeRateService;

    private final ExchangeRateProperties properties;

    private final Clock clock;

    /** No money moves and no balance is checked here: the ledger checks the balance when the quote is spent. */
    @Override
    @Transactional
    public ExchangeQuoteResponseDto createQuote(UUID userId, ExchangeQuoteRequestDto request) {

        AccountReferenceDto from = accountService.getOwnedAccountReference(userId, request.fromAccountId());
        AccountReferenceDto to = accountService.getOwnedAccountReference(userId, request.toAccountId());

        if (from.currency() == to.currency()) {
            throw new SameCurrencyExchangeException();
        }

        ConversionDto conversion = exchangeRateService.convert(from.currency(), to.currency(), request.amount());
        if (conversion.amount().signum() == 0) {
            throw new ExchangeAmountTooSmallException();
        }

        Instant now = clock.instant();
        ExchangeQuote quote = ExchangeQuote.create(userId, from.id(), to.id(), from.currency(), to.currency(),
                request.amount(), conversion.amount(), conversion.rate(), now, now.plus(properties.quoteTtl()));

        return ExchangeQuoteResponseDto.from(exchangeQuoteRepository.save(quote));
    }

    /** MANDATORY: without the caller's transaction the lock would be released right after this method. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ExchangeQuoteResponseDto lockForUse(UUID userId, UUID quoteId) {

        return exchangeQuoteRepository.findOwnedForUpdate(quoteId, userId)
                .map(ExchangeQuoteResponseDto::from)
                .orElseThrow(ExchangeQuoteNotFoundException::new);
    }
}
