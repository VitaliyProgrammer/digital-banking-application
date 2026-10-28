package io.github.vitaliyprogrammer.banking.payment.controller;

import io.github.vitaliyprogrammer.banking.payment.dto.request.CardTransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.shared.CurrentUserId;
import io.github.vitaliyprogrammer.banking.shared.OpenApiConfiguration;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Top-ups and transfers. Every payment request needs an Idempotency-Key header")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class PaymentController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String IDEMPOTENCY_KEY_DESCRIPTION = "A new unique value (e.g. a UUID) for every payment. "
            + "Repeating a request with the same key returns the original payment instead of paying twice";

    private final PaymentService paymentService;

    @PostMapping("/top-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Top up an account",
            description = "Credits one of your accounts with money from outside the bank. Free of charge")
    public PaymentResponseDto topUp(@CurrentUserId UUID userId,
                                    @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
                                    @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                    @Valid @RequestBody TopUpRequestDto request) {

        return paymentService.topUp(userId, idempotencyKey, request);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer to another client",
            description = "Sends money to a client account by IBAN in the same currency. "
                    + "Fee: 0.5%, minimum 5.00 UAH / 0.20 USD / 0.20 EUR")
    public PaymentResponseDto transfer(@CurrentUserId UUID userId,
                                       @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
                                       @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                       @Valid @RequestBody TransferRequestDto request) {

        return paymentService.transfer(userId, idempotencyKey, request);
    }

    @PostMapping("/card-transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer from card to card",
            description = "Sends money from your card to a 16-digit card number in the same currency. "
                    + "Fee depends on your card: PAYROLL 0.5%, STUDENT free, VIRTUAL 1%, CREDIT 2%; "
                    + "paid tariffs have a minimum of 5.00 UAH / 0.20 USD / 0.20 EUR")
    public PaymentResponseDto cardTransfer(@CurrentUserId UUID userId,
                                           @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
                                           @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                           @Valid @RequestBody CardTransferRequestDto request) {

        return paymentService.cardTransfer(userId, idempotencyKey, request);
    }

    @GetMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get payment",
            description = "One of your payments. Someone else's payment returns 404")
    public PaymentResponseDto getPayment(@CurrentUserId UUID userId,
                                         @PathVariable UUID paymentId) {

        return paymentService.getPayment(userId, paymentId);
    }
}
