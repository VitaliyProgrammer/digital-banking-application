package io.github.vitaliyprogrammer.banking.controller;

import io.github.vitaliyprogrammer.banking.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.dto.request.ExchangeQuoteRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.ExchangeQuoteResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.service.ExchangeQuoteService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Tag(name = "Currency exchange", description = "Exchange between your own accounts: get a quote, then accept it")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class ExchangeController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final ExchangeQuoteService exchangeQuoteService;

    private final PaymentService paymentService;

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Get an exchange quote",
            description = "Shows exactly how much you will get. The price holds for 30 seconds; no money moves yet")
    public ExchangeQuoteResponseDto createQuote(@CurrentUserId UUID userId,
                                                @Valid @RequestBody ExchangeQuoteRequestDto request) {

        return exchangeQuoteService.createQuote(userId, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Accept a quote",
            description = "Exchanges at the quoted price. 409 if the quote expired or was already used")
    public PaymentResponseDto exchange(@CurrentUserId UUID userId,
                                       @Parameter(description = "A new unique value (e.g. a UUID) for every exchange")
                                       @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                       @Valid @RequestBody ExchangeRequestDto request) {

        return paymentService.exchange(userId, idempotencyKey, request);
    }
}
