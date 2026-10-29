package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.SetBankRateRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.BankRateChangeResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.ConversionDto;
import io.github.vitaliyprogrammer.banking.dto.response.ExchangeRateResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.OfficialRateResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.RateSyncResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExchangeRateService {

    /** Loads today's NBU rates; a rate already stored for that day is skipped, so calling it again is harmless. */
    RateSyncResponseDto syncOfficialRates();

    /** The public rates table: official and bank rates of every quoted currency. */
    List<ExchangeRateResponseDto> getCurrentRates();

    /** Daily official rates for a chart; both dates inclusive, null means "last 30 days up to today". */
    List<OfficialRateResponseDto> getOfficialHistory(Currency currency, LocalDate from, LocalDate to);

    ExchangeRateResponseDto setBankRate(UUID adminId, Currency currency, SetBankRateRequestDto request);

    /**
     * Converts at the bank's current buy/sell rates; a pair without UAH goes through UAH.
     * The result is rounded down to cents, in the bank's favour.
     */
    ConversionDto convert(Currency from, Currency to, BigDecimal amount);

    /** Every change of the bank's rate, newest first, with the author. */
    PageResponseDto<BankRateChangeResponseDto> getBankRateChanges(Currency currency, int page, int size);
}
