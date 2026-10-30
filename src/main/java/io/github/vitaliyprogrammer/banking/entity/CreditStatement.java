package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.dto.response.SettlementDto;
import io.github.vitaliyprogrammer.banking.entity.status.CreditStatementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One month of a credit line ("2026-10"). Inserted natively when the month closes, then settled exactly once
 * after its due date: OPEN → PAID, MINIMUM_PAID or OVERDUE, with what was repaid and what was charged.
 */
@Entity
@Table(name = "credit_statements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditStatement {

    @Id
    private UUID id;

    private UUID accountId;

    private String period;

    @Column(precision = 19, scale = 2)
    private BigDecimal closingDebt;

    @Column(precision = 19, scale = 2)
    private BigDecimal minimumPayment;

    private Instant closedAt;

    private LocalDate dueDate;

    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    private CreditStatementStatus status;

    @Column(precision = 19, scale = 2)
    private BigDecimal repaid;

    @Column(precision = 19, scale = 2)
    private BigDecimal interest;

    @Column(precision = 19, scale = 2)
    private BigDecimal lateFee;

    private UUID ledgerTransactionId;

    private Instant settledAt;

    private Instant createdAt;

    public void settle(BigDecimal repaid, SettlementDto settlement, UUID ledgerTransactionId, Instant now) {

        if (status != CreditStatementStatus.OPEN) {
            throw new IllegalStateException("Statement " + id + " is already settled: " + status);
        }

        this.status = settlement.status();
        this.repaid = repaid;
        this.interest = settlement.interest();
        this.lateFee = settlement.lateFee();
        this.ledgerTransactionId = ledgerTransactionId;
        this.settledAt = now;
    }
}
