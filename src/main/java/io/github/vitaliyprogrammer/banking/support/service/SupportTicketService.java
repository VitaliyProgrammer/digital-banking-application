package io.github.vitaliyprogrammer.banking.support.service;

import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.support.dto.request.OpenTicketRequestDto;
import io.github.vitaliyprogrammer.banking.support.dto.request.TicketReplyRequestDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.SupportClientViewDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.TicketDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.TicketResponseDto;
import io.github.vitaliyprogrammer.banking.support.entity.status.TicketStatus;
import java.util.List;
import java.util.UUID;

public interface SupportTicketService {

    // The client's side: only their own tickets, someone else's is "not found".

    TicketDetailsResponseDto open(UUID clientId, OpenTicketRequestDto request);

    List<TicketResponseDto> getMyTickets(UUID clientId);

    TicketDetailsResponseDto getMyTicket(UUID clientId, UUID ticketId);

    TicketDetailsResponseDto replyAsClient(UUID clientId, UUID ticketId, TicketReplyRequestDto request);

    // The staff side (SUPPORT, ADMIN).

    PageResponseDto<TicketResponseDto> getTickets(TicketStatus status, int page, int size);

    TicketDetailsResponseDto getTicket(UUID ticketId);

    TicketDetailsResponseDto take(UUID staffId, UUID ticketId);

    TicketDetailsResponseDto replyAsStaff(UUID staffId, UUID ticketId, TicketReplyRequestDto request);

    TicketDetailsResponseDto resolve(UUID staffId, UUID ticketId);

    /** The client's profile and accounts, read-only, and only for the employee who took this ticket. */
    SupportClientViewDto getClientView(UUID staffId, UUID ticketId);
}
