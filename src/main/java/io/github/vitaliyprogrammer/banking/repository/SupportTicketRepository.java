package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.SupportTicket;
import io.github.vitaliyprogrammer.banking.entity.status.TicketStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);

    Optional<SupportTicket> findByIdAndClientId(UUID id, UUID clientId);

    /** The queue (OPEN) oldest first, or any other status for a staff overview. */
    Page<SupportTicket> findAllByStatusOrderByCreatedAt(TicketStatus status, Pageable pageable);

    /** Every change of a ticket goes through this lock: a second employee waits, then sees the new state. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SupportTicket t where t.id = :id")
    Optional<SupportTicket> findByIdForUpdate(UUID id);
}
