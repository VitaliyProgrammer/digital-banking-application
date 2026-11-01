package io.github.vitaliyprogrammer.banking.card.repository;

import io.github.vitaliyprogrammer.banking.card.entity.Card;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findAllByOwnerIdOrderByCreatedAt(UUID ownerId);

    Optional<Card> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Card> findByPanLookupHash(String panLookupHash);

    boolean existsByPanLookupHash(String panLookupHash);

    boolean existsByAccountIdAndTypeAndStatusNot(UUID accountId, CardType type, CardStatus status);

    /**
     * PIN checks of one card run one after another. The attempt count itself is atomic in Redis;
     * the lock keeps the status change (ACTIVE → PIN_LOCKED) from racing with another check or a parallel block.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id and c.ownerId = :ownerId")
    Optional<Card> findByIdAndOwnerIdForUpdate(UUID id, UUID ownerId);
}
