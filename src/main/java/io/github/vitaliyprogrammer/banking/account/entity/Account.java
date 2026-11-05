package io.github.vitaliyprogrammer.banking.account.entity;

import io.github.vitaliyprogrammer.banking.account.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotActiveException;
import io.github.vitaliyprogrammer.banking.account.exception.CreditLineAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
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

    /**
     * How far below zero the balance may go: an approved credit limit. It lives on the client's own UAH account
     * (like monobank), so the same balance shows own money above zero and the debt below it. Accounts of the
     * earlier model (a separate CREDIT account) carry it too and work the same way.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal creditLimit;

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

    public static Account openSavingsAccount(UUID ownerId, String number, Currency currency, Instant now) {
        Account account = openClientAccount(ownerId, number, currency, now);
        account.type = AccountType.SAVINGS;
        return account;
    }

    /** Credit is in hryvnias only (the limit comes from income in hryvnias), and an account gets it once. */
    public void grantCreditLimit(BigDecimal limit) {
        if (type != AccountType.CLIENT || currency != Currency.UAH) {
            throw new IllegalStateException("A credit limit goes on a UAH current account only");
        }
        if (creditLimit != null) {
            throw new CreditLineAlreadyExistsException();
        }
        creditLimit = limit;
    }

    /** Accounts that belong to a client: current, savings and credit. */
    public boolean isClientOwned() {
        return type == AccountType.CLIENT || type == AccountType.SAVINGS || type == AccountType.CREDIT;
    }

    /**
     * The only way to change the balance. Called by the ledger, never directly by other code, and always under the
     * row lock: two parallel payments from one credit card cannot both slip under the limit.
     * The transaction type matters only on a credit line: card payments and bills may use the limit, other money
     * movements only the client's own money, and the bank's charges may even go past the limit.
     */
    public void apply(TransactionType transactionType, EntryDirection direction, BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(number);
        }

        BigDecimal newBalance = direction == EntryDirection.CREDIT
                ? balance.add(amount)
                : balance.subtract(amount);

        if (direction == EntryDirection.DEBIT && isClientOwned() && !mayGoTo(newBalance, transactionType)) {
            throw new InsufficientFundsException();
        }
        balance = newBalance;
    }

    /** Zero for own money; minus the limit when credit may be used; no floor for the bank's charges. */
    private boolean mayGoTo(BigDecimal newBalance, TransactionType transactionType) {
        if (creditLimit == null || !transactionType.mayUseCredit()) {
            return newBalance.signum() >= 0;
        }
        return transactionType.mayExceedCreditLimit() || newBalance.compareTo(creditLimit.negate()) >= 0;
    }
}
