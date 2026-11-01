package io.github.vitaliyprogrammer.banking.cashback.dto.response;

import io.github.vitaliyprogrammer.banking.cashback.entity.CashbackAccrual;
import io.github.vitaliyprogrammer.banking.payment.entity.status.ProviderCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A month of cashback: what was earned, how much of the cap is left, whether it was paid, and for what. */
public record CashbackResponseDto(
        String period,
        BigDecimal earned,
        BigDecimal monthlyCap,
        BigDecimal remaining,
        boolean paidOut,
        Map<ProviderCategory, BigDecimal> rates,
        List<Item> items
) {

    public record Item(
            UUID paymentId,
            ProviderCategory category,
            BigDecimal paymentAmount,
            BigDecimal rate,
            BigDecimal amount,
            Instant earnedAt
    ) {

        public static Item from(CashbackAccrual accrual) {
            return new Item(
                    accrual.getPaymentId(),
                    accrual.getCategory(),
                    accrual.getPaymentAmount().setScale(2),
                    accrual.getRate(),
                    accrual.getAmount(),
                    accrual.getCreatedAt());
        }
    }
}
