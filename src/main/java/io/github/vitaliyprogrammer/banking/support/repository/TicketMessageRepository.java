package io.github.vitaliyprogrammer.banking.support.repository;

import io.github.vitaliyprogrammer.banking.support.entity.TicketMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findAllByTicketIdOrderByCreatedAt(UUID ticketId);
}
