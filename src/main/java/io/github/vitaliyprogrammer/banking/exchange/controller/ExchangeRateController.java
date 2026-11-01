package io.github.vitaliyprogrammer.banking.exchange.controller;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.ExchangeRateResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.dto.response.OfficialRateResponseDto;
import io.github.vitaliyprogrammer.banking.exchange.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public, like the rates table on a bank's home page: no token needed. */
@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange rates", description = "Official NBU rates and the bank's buy/sell rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get current rates",
            description = "NBU and bank rates of every currency. stale = true: today's NBU rate is not loaded yet")
    public List<ExchangeRateResponseDto> getCurrentRates() {

        return exchangeRateService.getCurrentRates();
    }

    @GetMapping("/{currency}/history")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get official rate history",
            description = "Daily NBU rates for a chart. Defaults to the last 30 days, at most 366 days")
    public List<OfficialRateResponseDto> getOfficialHistory(@PathVariable Currency currency,
                                                            @RequestParam(required = false) LocalDate from,
                                                            @RequestParam(required = false) LocalDate to) {

        return exchangeRateService.getOfficialHistory(currency, from, to);
    }
}
