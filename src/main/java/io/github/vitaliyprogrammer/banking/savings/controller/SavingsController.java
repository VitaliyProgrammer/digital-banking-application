package io.github.vitaliyprogrammer.banking.savings.controller;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import io.github.vitaliyprogrammer.banking.savings.dto.request.SavingsTransferRequestDto;
import io.github.vitaliyprogrammer.banking.savings.dto.response.SavingsResponseDto;
import io.github.vitaliyprogrammer.banking.savings.service.SavingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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
@RequestMapping("/savings")
@RequiredArgsConstructor
@Tag(name = "Savings", description = "Savings jars that earn interest, paid monthly")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class SavingsController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final SavingsService savingsService;

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a savings jar",
            description = "One jar per currency. Interest is accrued daily and paid on the 1st of every month")
    public SavingsResponseDto open(@CurrentUserId UUID userId,
                                   @Valid @RequestBody OpenAccountRequestDto request) {

        return savingsService.open(userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my savings",
            description = "Balance, annual rate and interest accrued but not paid yet")
    public List<SavingsResponseDto> getSavings(@CurrentUserId UUID userId) {

        return savingsService.getSavings(userId);
    }

    @PostMapping("/{savingsId}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Put money into a jar",
            description = "From your current account in the same currency. Free of charge")
    public PaymentResponseDto deposit(@CurrentUserId UUID userId,
                                      @PathVariable UUID savingsId,
                                      @Parameter(description = "A new unique value (e.g. a UUID) for every operation")
                                      @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                      @Valid @RequestBody SavingsTransferRequestDto request) {

        return paymentService.depositToSavings(userId, idempotencyKey, savingsId, request);
    }

    @PostMapping("/{savingsId}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Take money out of a jar",
            description = "Back to your current account in the same currency. Free of charge")
    public PaymentResponseDto withdraw(@CurrentUserId UUID userId,
                                       @PathVariable UUID savingsId,
                                       @Parameter(description = "A new unique value (e.g. a UUID) for every operation")
                                       @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                       @Valid @RequestBody SavingsTransferRequestDto request) {

        return paymentService.withdrawFromSavings(userId, idempotencyKey, savingsId, request);
    }
}
