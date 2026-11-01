package io.github.vitaliyprogrammer.banking.account.repository;

import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByOwnerIdOrderByCreatedAt(UUID ownerId);

    Optional<Account> findByIdAndOwnerIdAndType(UUID id, UUID ownerId, AccountType type);

    Optional<Account> findByIdAndOwnerIdAndTypeIn(UUID id, UUID ownerId, Collection<AccountType> types);

    Optional<Account> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** For CREDIT: at most one per owner (partial unique index). */
    Optional<Account> findByOwnerIdAndType(UUID ownerId, AccountType type);

    boolean existsByOwnerIdAndCurrencyAndType(UUID ownerId, Currency currency, AccountType type);

    /** A bank account type may have several shards (see V22); every one of them. */
    List<Account> findAllByTypeAndCurrency(AccountType type, Currency currency);

    Optional<Account> findByNumberAndType(String number, AccountType type);

    List<Account> findAllByTypeAndStatus(AccountType type, AccountStatus status);

    /**
     * Locks all accounts of one posting in a single query, always in the same order (by id).
     * Two transfers A→B and B→A therefore lock A and B in the same sequence and cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id in :ids order by a.id")
    List<Account> findAllByIdForUpdate(Collection<UUID> ids);
}
