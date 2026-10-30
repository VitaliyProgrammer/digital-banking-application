package io.github.vitaliyprogrammer.banking.controller;

import io.github.vitaliyprogrammer.banking.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.dto.request.CreditRepaymentRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditLineResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.CreditStatementResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.service.CreditLineService;
import io.github.vitaliyprogrammer.banking.service.PaymentService;
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
@RequestMapping("/credit-line")
@RequiredArgsConstructor
@Tag(name = "Credit line", description = "The approved credit limit: spend it with the CREDIT card, repay it")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class CreditLineController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final CreditLineService creditLineService;

    private final PaymentService paymentService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my credit line",
            description = "Limit, debt and what is still available. Money spent in a month is interest-free "
                    + "until the end of the next month, up to 62 days")
    public CreditLineResponseDto getCreditLine(@CurrentUserId UUID userId) {

        return creditLineService.getCreditLine(userId);
    }

    @GetMapping("/statements")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my credit statements",
            description = "One per month, newest first: the debt at the end of the month, the minimum payment, "
                    + "the due date and, after it, what was repaid and charged")
    public List<CreditStatementResponseDto> getStatements(@CurrentUserId UUID userId) {

        return creditLineService.getStatements(userId);
    }

    @PostMapping("/repayments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Repay the credit line",
            description = "From your UAH current account. Free of charge; never more than the debt")
    public PaymentResponseDto repay(@CurrentUserId UUID userId,
                                    @Parameter(description = "A new unique value (e.g. a UUID) for every operation")
                                    @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
                                    @Valid @RequestBody CreditRepaymentRequestDto request) {

        return paymentService.repayCredit(userId, idempotencyKey, request);
    }
}
