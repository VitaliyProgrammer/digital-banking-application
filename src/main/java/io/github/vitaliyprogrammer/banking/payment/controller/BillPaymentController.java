package io.github.vitaliyprogrammer.banking.payment.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.payment.dto.request.BillPaymentRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.payment.dto.response.ProviderResponseDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bill-payments")
@RequiredArgsConstructor
@Tag(name = "Bill payments", description = "Utilities, internet and mobile top-ups, paid to external providers")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class BillPaymentController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final PaymentService paymentService;

    @GetMapping("/providers")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get providers",
            description = "Companies you can pay: electricity, water, gas, heating, internet, mobile operators")
    public List<ProviderResponseDto> getProviders() {

        return paymentService.getProviders();
    }

    /** 202 Accepted: the request is taken, but the result comes later. Poll GET /payments/{id} for the status. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pay a bill",
            description = "Holds the money and answers at once with status PROCESSING. The bank then pays the "
                    + "provider in the background: COMPLETED, or FAILED with the money returned. "
                    + "Check the result with GET /payments/{paymentId}")
    public PaymentResponseDto payBill(@CurrentUserId UUID userId,
                                      @Parameter(description = "A new unique value (e.g. a UUID) for every payment")
                                      @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                      @Valid @RequestBody BillPaymentRequestDto request) {

        return paymentService.payBill(userId, idempotencyKey, request);
    }
}
