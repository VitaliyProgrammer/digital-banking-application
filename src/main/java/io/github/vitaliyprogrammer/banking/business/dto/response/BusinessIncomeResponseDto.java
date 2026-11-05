package io.github.vitaliyprogrammer.banking.business.dto.response;

import io.github.vitaliyprogrammer.banking.business.entity.BusinessIncome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BusinessIncomeResponseDto(
        UUID paymentId,
        BigDecimal amount,
        /* Put into the tax jar at the same moment. */
        BigDecimal tax,
        Instant createdAt
) {

    public static BusinessIncomeResponseDto from(BusinessIncome income) {
        return new BusinessIncomeResponseDto(income.getPaymentId(), income.getAmount(), income.getTax(),
                income.getCreatedAt());
    }
}
