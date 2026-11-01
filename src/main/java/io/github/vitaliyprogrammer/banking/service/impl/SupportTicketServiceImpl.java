package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.OpenTicketRequestDto;
import io.github.vitaliyprogrammer.banking.dto.request.TicketReplyRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.SupportClientViewDto;
import io.github.vitaliyprogrammer.banking.dto.response.TicketDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.TicketMessageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.TicketResponseDto;
import io.github.vitaliyprogrammer.banking.entity.SupportTicket;
import io.github.vitaliyprogrammer.banking.entity.TicketMessage;
import io.github.vitaliyprogrammer.banking.entity.status.AuditAction;
import io.github.vitaliyprogrammer.banking.entity.status.AuditTarget;
import io.github.vitaliyprogrammer.banking.entity.status.TicketStatus;
import io.github.vitaliyprogrammer.banking.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.event.SupportTicketMessage;
import io.github.vitaliyprogrammer.banking.exception.TicketNotFoundException;
import io.github.vitaliyprogrammer.banking.repository.SupportTicketRepository;
import io.github.vitaliyprogrammer.banking.repository.TicketMessageRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
import io.github.vitaliyprogrammer.banking.service.AuditService;
import io.github.vitaliyprogrammer.banking.service.OutboxService;
import io.github.vitaliyprogrammer.banking.service.SupportTicketService;
import io.github.vitaliyprogrammer.banking.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;

    private final TicketMessageRepository ticketMessageRepository;

    private final UserService userService;

    private final AccountService accountService;

    private final OutboxService outboxService;

    private final AuditService auditService;

    private final Clock clock;

    @Override
    @Transactional
    public TicketDetailsResponseDto open(UUID clientId, OpenTicketRequestDto request) {

        Instant now = clock.instant();
        SupportTicket ticket = supportTicketRepository.save(
                SupportTicket.open(clientId, request.subject().strip(), now));
        ticketMessageRepository.save(TicketMessage.of(ticket.getId(), clientId, false, request.message().strip(), now));

        return details(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getMyTickets(UUID clientId) {

        return supportTicketRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(TicketResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailsResponseDto getMyTicket(UUID clientId, UUID ticketId) {

        return details(supportTicketRepository.findByIdAndClientId(ticketId, clientId)
                .orElseThrow(TicketNotFoundException::new));
    }

    @Override
    @Transactional
    public TicketDetailsResponseDto replyAsClient(UUID clientId, UUID ticketId, TicketReplyRequestDto request) {

        SupportTicket ticket = lock(ticketId);
        if (!ticket.getClientId().equals(clientId)) {
            throw new TicketNotFoundException();
        }

        Instant now = clock.instant();
        ticket.recordClientReply(now);
        ticketMessageRepository.save(TicketMessage.of(ticketId, clientId, false, request.message().strip(), now));

        return details(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<TicketResponseDto> getTickets(TicketStatus status, int page, int size) {

        return PageResponseDto.from(
                supportTicketRepository.findAllByStatusOrderByCreatedAt(status, PageRequest.of(page, size)),
                TicketResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailsResponseDto getTicket(UUID ticketId) {

        return details(supportTicketRepository.findById(ticketId).orElseThrow(TicketNotFoundException::new));
    }

    /** Two employees clicking "take" at once: the second waits for the lock, then finds it taken and gets 409. */
    @Override
    @Transactional
    public TicketDetailsResponseDto take(UUID staffId, UUID ticketId) {

        SupportTicket ticket = lock(ticketId);
        ticket.take(staffId, clock.instant());
        audit(staffId, AuditAction.TICKET_TAKEN, ticket, Map.of("from", "OPEN", "to", "IN_PROGRESS"));

        log.info("Ticket {} taken by {}", ticketId, staffId);
        return details(ticket);
    }

    @Override
    @Transactional
    public TicketDetailsResponseDto replyAsStaff(UUID staffId, UUID ticketId, TicketReplyRequestDto request) {

        SupportTicket ticket = lock(ticketId);
        Instant now = clock.instant();
        ticket.recordStaffReply(staffId, now);
        TicketMessage answer = ticketMessageRepository.save(
                TicketMessage.of(ticketId, staffId, true, request.message().strip(), now));
        audit(staffId, AuditAction.TICKET_ANSWERED, ticket, Map.of("messageId", answer.getId()));

        outboxService.record(OutboxEventType.SUPPORT_TICKET_REPLIED, ticketId, SupportTicketMessage.from(ticket));
        return details(ticket);
    }

    @Override
    @Transactional
    public TicketDetailsResponseDto resolve(UUID staffId, UUID ticketId) {

        SupportTicket ticket = lock(ticketId);
        ticket.resolve(staffId, clock.instant());
        audit(staffId, AuditAction.TICKET_RESOLVED, ticket, Map.of("from", "IN_PROGRESS", "to", "RESOLVED"));

        outboxService.record(OutboxEventType.SUPPORT_TICKET_RESOLVED, ticketId, SupportTicketMessage.from(ticket));
        log.info("Ticket {} resolved by {}", ticketId, staffId);
        return details(ticket);
    }

    /**
     * Least privilege: support has no general "show me any client's money" screen. An employee sees a client's
     * accounts because they are working on that client's ticket, and only while it is theirs.
     * Not read-only: looking at a client's money is itself written to the audit log.
     */
    @Override
    @Transactional
    public SupportClientViewDto getClientView(UUID staffId, UUID ticketId) {

        SupportTicket ticket = supportTicketRepository.findById(ticketId).orElseThrow(TicketNotFoundException::new);
        ticket.requireAssignee(staffId);

        auditService.record(staffId, AuditAction.CLIENT_ACCOUNTS_VIEWED, AuditTarget.CLIENT, ticket.getClientId(),
                Map.of("ticketId", ticketId));
        return new SupportClientViewDto(
                userService.getById(ticket.getClientId()),
                accountService.getAccounts(ticket.getClientId()));
    }

    private void audit(UUID staffId, AuditAction action, SupportTicket ticket, Map<String, ?> details) {

        auditService.record(staffId, action, AuditTarget.SUPPORT_TICKET, ticket.getId(), details);
    }

    private SupportTicket lock(UUID ticketId) {

        return supportTicketRepository.findByIdForUpdate(ticketId).orElseThrow(TicketNotFoundException::new);
    }

    private TicketDetailsResponseDto details(SupportTicket ticket) {

        return new TicketDetailsResponseDto(
                TicketResponseDto.from(ticket),
                ticketMessageRepository.findAllByTicketIdOrderByCreatedAt(ticket.getId()).stream()
                        .map(TicketMessageResponseDto::from)
                        .toList());
    }
}
