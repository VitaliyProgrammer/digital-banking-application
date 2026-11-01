package io.github.vitaliyprogrammer.banking.exchange.controller;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.exchange.dto.request.SetBankRateRequestDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.BankRateChangeResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.ExchangeRateResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.RateSyncResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /admin requires the ADMIN role (see SecurityConfiguration). */
@RestController
@RequestMapping("/admin/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Admin: exchange rates", description = "Setting the bank's buy/sell rates. ADMIN only")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @PutMapping("/{currency}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Set the bank rate",
            description = "SPREAD: a percentage around the NBU rate, follows it every day. MANUAL: exact buy and sell")
    public ExchangeRateResponseDto setBankRate(@CurrentUserId UUID adminId,
                                               @PathVariable Currency currency,
                                               @Valid @RequestBody SetBankRateRequestDto request) {

        return exchangeRateService.setBankRate(adminId, currency, request);
    }

    @GetMapping("/{currency}/changes")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get rate change history",
            description = "Every change of the bank rate, newest first, with who made it")
    public PageResponseDto<BankRateChangeResponseDto> getChanges(
            @PathVariable Currency currency,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return exchangeRateService.getBankRateChanges(currency, page, size);
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Sync NBU rates now",
            description = "Loads today's NBU rates without waiting for the schedule. 503 if the NBU does not answer")
    public RateSyncResponseDto sync() {

        return exchangeRateService.syncOfficialRates();
    }
}
