package io.github.vitaliyprogrammer.banking.cashback.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A month's cashback paid to one account. Inserted natively, before the money moves. */
@Entity
@Table(name = "cashback_payouts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashbackPayout {

    @Id
    private UUID id;

    private UUID accountId;

    private String period;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private UUID ledgerTransactionId;

    private Instant paidAt;

    public void attachLedgerTransaction(UUID ledgerTransactionId) {
        this.ledgerTransactionId = ledgerTransactionId;
    }
}
