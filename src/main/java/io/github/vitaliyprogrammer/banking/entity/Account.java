package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.exception.AccountNotActiveException;
import io.github.vitaliyprogrammer.banking.exception.InsufficientFundsException;
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

/**
 * The balance is a cached projection of the ledger: it changes only together with a ledger entry,
 * inside the same transaction and under a row lock (see {@code LedgerServiceImpl}).
 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID ownerId;

    private String number;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    private Instant createdAt;

    public static Account openClientAccount(UUID ownerId, String number, Currency currency, Instant now) {
        Account account = new Account();
        account.ownerId = ownerId;
        account.number = number;
        account.currency = currency;
        account.type = AccountType.CLIENT;
        account.status = AccountStatus.ACTIVE;
        account.balance = BigDecimal.ZERO;
        account.createdAt = now;
        return account;
    }

    /** The only way to change the balance. Called by the ledger, never directly by other code. */
    public void apply(EntryDirection direction, BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(number);
        }

        BigDecimal newBalance = direction == EntryDirection.CREDIT
                ? balance.add(amount)
                : balance.subtract(amount);

        if (type == AccountType.CLIENT && newBalance.signum() < 0) {
            throw new InsufficientFundsException();
        }
        balance = newBalance;
    }
}
