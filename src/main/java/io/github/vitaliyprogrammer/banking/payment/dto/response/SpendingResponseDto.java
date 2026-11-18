package io.github.vitaliyprogrammer.banking.payment.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SpendingResponseDto(
        /* Both days included, in Kyiv time. */
        LocalDate from,
        LocalDate to,
        /* Only hryvnia payments are counted: sums in different currencies cannot be added up. */
        Currency currency,
        BigDecimal total,
        /* Largest first; a category with no payments is left out. */
        List<CategoryTotal> categories
) {

    public record CategoryTotal(
            SpendingCategory category,
            BigDecimal amount
    ) {
    }
}
