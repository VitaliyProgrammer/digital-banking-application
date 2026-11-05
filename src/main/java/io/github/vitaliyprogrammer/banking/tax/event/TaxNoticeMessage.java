package io.github.vitaliyprogrammer.banking.tax.event;

import io.github.vitaliyprogrammer.banking.tax.entity.status.TaxNoticeKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** The body of tax.notice: a reminder before the due date, or "not enough money" on the day of auto-pay. */
public record TaxNoticeMessage(
        UUID userId,
        TaxNoticeKind kind,
        /* For people: "11.2026" or "IV квартал 2026". */
        String period,
        /* Still to pay for the period. */
        BigDecimal amount,
        LocalDate dueDate,
        boolean autoPay
) {
}
