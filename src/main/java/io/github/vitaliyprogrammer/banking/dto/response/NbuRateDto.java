package io.github.vitaliyprogrammer.banking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * One item of the NBU JSON, e.g. {"r030":840, "txt":"...", "rate":41.4729, "cc":"USD", "exchangedate":"29.10.2026"}.
 * Field names follow the NBU API, not our style; fields we do not need (r030, txt) are ignored.
 */
public record NbuRateDto(
        String cc,
        BigDecimal rate,
        String exchangedate
) {

    private static final DateTimeFormatter NBU_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public LocalDate date() {
        return LocalDate.parse(exchangedate, NBU_DATE);
    }
}
