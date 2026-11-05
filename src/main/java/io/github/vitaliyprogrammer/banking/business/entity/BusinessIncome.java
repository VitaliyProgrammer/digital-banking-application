package io.github.vitaliyprogrammer.banking.business.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;

/**
 * One income of a sole proprietor and the tax put aside from it. Written in the same transaction as the payment,
 * never changed: the quarter's income and tax are sums of these rows.
 */
@Entity
@Immutable
@Table(name = "business_incomes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessIncome {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID businessId;

    private UUID paymentId;

    private BigDecimal amount;

    private BigDecimal tax;

    private Instant createdAt;

    public static BusinessIncome of(UUID businessId, UUID paymentId, BigDecimal amount, BigDecimal tax, Instant now) {
        BusinessIncome income = new BusinessIncome();
        income.businessId = businessId;
        income.paymentId = paymentId;
        income.amount = amount;
        income.tax = tax;
        income.createdAt = now;
        return income;
    }
}
