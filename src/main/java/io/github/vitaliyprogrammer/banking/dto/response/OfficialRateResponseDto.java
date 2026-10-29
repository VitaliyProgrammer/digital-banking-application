package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.OfficialRate;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One point of the official rate chart. */
public record OfficialRateResponseDto(
        LocalDate date,
        BigDecimal rate
) {

    public static OfficialRateResponseDto from(OfficialRate rate) {
        return new OfficialRateResponseDto(rate.getRateDate(), rate.getRate());
    }
}
