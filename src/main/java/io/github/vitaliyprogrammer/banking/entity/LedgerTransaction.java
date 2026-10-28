package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.entity.status.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * One business operation in the journal (a deposit, a transfer). Its entries always balance:
 * total debited = total credited. Immutable once saved; the database rejects any UPDATE or DELETE.
 */
@Entity
@Table(name = "ledger_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerTransaction {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String description;

    private Instant createdAt;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.PERSIST)
    private List<LedgerEntry> entries = new ArrayList<>();

    public static LedgerTransaction create(TransactionType type, String description, Instant now) {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.type = type;
        transaction.description = description;
        transaction.createdAt = now;
        return transaction;
    }

    public void addEntry(UUID accountId, EntryDirection direction, BigDecimal amount, BigDecimal balanceAfter) {
        entries.add(LedgerEntry.create(this, accountId, direction, amount, balanceAfter, createdAt));
    }
}
