package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.ProviderCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** Cashback earned for one completed bill payment; paid out with the rest of the month's cashback. */
@Entity
@Table(name = "cashback_accruals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashbackAccrual {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID userId;

    private UUID paymentId;

    /** Where the cashback goes: the account the payment was paid from. */
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private ProviderCategory category;

    @Column(precision = 19, scale = 4)
    private BigDecimal paymentAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    /** The month it belongs to, e.g. "2026-10" (Kyiv time of the payment). */
    private String period;

    private Instant createdAt;

    public static CashbackAccrual earn(UUID userId, UUID paymentId, UUID accountId, ProviderCategory category,
                                       BigDecimal paymentAmount, BigDecimal rate, BigDecimal amount,
                                       String period, Instant now) {
        CashbackAccrual accrual = new CashbackAccrual();
        accrual.userId = userId;
        accrual.paymentId = paymentId;
        accrual.accountId = accountId;
        accrual.category = category;
        accrual.paymentAmount = paymentAmount;
        accrual.rate = rate;
        accrual.amount = amount;
        accrual.period = period;
        accrual.createdAt = now;
        return accrual;
    }
}
