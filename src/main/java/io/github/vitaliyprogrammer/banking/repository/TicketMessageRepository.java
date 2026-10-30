package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.TicketMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findAllByTicketIdOrderByCreatedAt(UUID ticketId);
}
