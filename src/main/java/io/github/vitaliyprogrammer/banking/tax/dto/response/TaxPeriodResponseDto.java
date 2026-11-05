package io.github.vitaliyprogrammer.banking.tax.dto.response;

import io.github.vitaliyprogrammer.banking.tax.entity.status.TaxPeriodStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One tax period of a sole proprietor: how much, by when, and how much is paid. */
public record TaxPeriodResponseDto(
        /* "2026-11" (groups 1 and 2) or "2026-Q4" (group 3): the id to pay it by. */
        String period,
        /* For people: "11.2026" or "IV квартал 2026". */
        String label,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paid,
        BigDecimal left,
        TaxPeriodStatus status
) {
}
