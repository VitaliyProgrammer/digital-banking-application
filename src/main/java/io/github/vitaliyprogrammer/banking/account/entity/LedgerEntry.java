package io.github.vitaliyprogrammer.banking.account.entity;

import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** One line of a ledger transaction: how much left or entered one account, and the balance right after. */
@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private LedgerTransaction transaction;

    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private EntryDirection direction;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    private Instant createdAt;

    static LedgerEntry create(LedgerTransaction transaction,
                              UUID accountId,
                              EntryDirection direction,
                              BigDecimal amount,
                              BigDecimal balanceAfter,
                              Instant now) {
        LedgerEntry entry = new LedgerEntry();
        entry.transaction = transaction;
        entry.accountId = accountId;
        entry.direction = direction;
        entry.amount = amount;
        entry.balanceAfter = balanceAfter;
        entry.createdAt = now;
        return entry;
    }
}
