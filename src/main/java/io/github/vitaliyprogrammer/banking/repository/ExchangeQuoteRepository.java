package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.ExchangeQuote;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ExchangeQuoteRepository extends JpaRepository<ExchangeQuote, UUID> {

    /** Someone else's quote is "not found", like someone else's account. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from ExchangeQuote q where q.id = :id and q.ownerId = :ownerId")
    Optional<ExchangeQuote> findOwnedForUpdate(UUID id, UUID ownerId);
}
