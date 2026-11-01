package io.github.vitaliyprogrammer.banking.cashback.controller;

import io.github.vitaliyprogrammer.banking.cashback.dto.response.CashbackResponseDto;
import io.github.vitaliyprogrammer.banking.cashback.service.CashbackService;
import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cashback")
@RequiredArgsConstructor
@Tag(name = "Cashback", description = "Cashback for paying bills: mobile, internet and utilities")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class CashbackController {

    private final CashbackService cashbackService;

    private final Clock clock;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my cashback",
            description = "Earned in a month, what is left of the monthly cap, and whether it was paid. "
                    + "Cashback is paid on the 1st of the next month")
    public CashbackResponseDto getCashback(@CurrentUserId UUID userId,
                                           @Parameter(description = "Month as yyyy-MM; the current month by default")
                                           @RequestParam(required = false) @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])")
                                           String period) {

        YearMonth month = period != null ? YearMonth.parse(period) : YearMonth.now(clock.withZone(ZoneId.of("Europe/Kyiv")));

        return cashbackService.getCashback(userId, month);
    }
}
