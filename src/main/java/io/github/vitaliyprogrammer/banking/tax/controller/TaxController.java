package io.github.vitaliyprogrammer.banking.tax.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.payment.dto.response.PaymentResponseDto;
import io.github.vitaliyprogrammer.banking.tax.dto.response.TaxPeriodResponseDto;
import io.github.vitaliyprogrammer.banking.tax.service.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
@Tag(name = "Single tax", description = "A sole proprietor's tax periods, due dates and payments")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class TaxController {

    private final TaxService taxService;

    @GetMapping("/periods")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my tax periods",
            description = "Groups 1 and 2: months, due by the 20th. Group 3: quarters, due 50 days after the quarter")
    public List<TaxPeriodResponseDto> getPeriods(@CurrentUserId UUID userId) {

        return taxService.getPeriods(userId);
    }

    @PostMapping("/periods/{period}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pay a tax period now",
            description = "Pays what is left from the tax jar, or the business account if the jar is short. "
                    + "Nothing is paid in part. Free of charge")
    public PaymentResponseDto pay(@CurrentUserId UUID userId, @PathVariable String period) {

        return taxService.payNow(userId, period);
    }
}
