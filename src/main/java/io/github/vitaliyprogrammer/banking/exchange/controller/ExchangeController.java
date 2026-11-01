package io.github.vitaliyprogrammer.banking.exchange.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.exchange.dto.request.ExchangeQuoteRequestDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.ExchangeQuoteResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Prices an exchange; accepting the quote is a payment (see the payment domain's CurrencyExchangeController). */
@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Tag(name = "Currency exchange", description = "Exchange between your own accounts: get a quote, then accept it")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class ExchangeController {

    private final ExchangeQuoteService exchangeQuoteService;

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Get an exchange quote",
            description = "Shows exactly how much you will get. The price holds for 30 seconds; no money moves yet")
    public ExchangeQuoteResponseDto createQuote(@CurrentUserId UUID userId,
                                                @Valid @RequestBody ExchangeQuoteRequestDto request) {

        return exchangeQuoteService.createQuote(userId, request);
    }
}
