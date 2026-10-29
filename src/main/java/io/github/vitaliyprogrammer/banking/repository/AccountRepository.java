package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.Account;
import io.github.vitaliyprogrammer.banking.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
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

    Optional<Account> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndCurrencyAndType(UUID ownerId, Currency currency, AccountType type);

    Optional<Account> findByTypeAndCurrency(AccountType type, Currency currency);

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
