package io.github.vitaliyprogrammer.banking.payment.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.payment.dto.request.ExchangeRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
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

/**
 * Accepting a quote moves money, so it belongs to payments: an exchange is a payment with idempotency, a ledger
 * posting and a receipt. The exchange domain only prices it (rates and quotes) and never calls payments back.
 * Same URL and the same Swagger tag as the quotes, so for a client nothing changed.
 */
@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Tag(name = "Currency exchange", description = "Exchange between your own accounts: get a quote, then accept it")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class CurrencyExchangeController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final PaymentService paymentService;

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
