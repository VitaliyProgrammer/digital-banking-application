package io.github.vitaliyprogrammer.banking.support.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.support.dto.request.OpenTicketRequestDto;
import io.github.vitaliyprogrammer.banking.support.dto.request.TicketReplyRequestDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.TicketDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.TicketResponseDto;
import io.github.vitaliyprogrammer.banking.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Ask the bank's support and follow the conversation")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a ticket", description = "The ticket goes to the support queue")
    public TicketDetailsResponseDto open(@CurrentUserId UUID userId,
                                         @Valid @RequestBody OpenTicketRequestDto request) {

        return supportTicketService.open(userId, request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my tickets", description = "Newest first")
    public List<TicketResponseDto> getMyTickets(@CurrentUserId UUID userId) {

        return supportTicketService.getMyTickets(userId);
    }

    @GetMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my ticket", description = "With the whole conversation")
    public TicketDetailsResponseDto getMyTicket(@CurrentUserId UUID userId, @PathVariable UUID ticketId) {

        return supportTicketService.getMyTicket(userId, ticketId);
    }

    @PostMapping("/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Write to support", description = "Not possible once the ticket is resolved")
    public TicketDetailsResponseDto reply(@CurrentUserId UUID userId,
                                          @PathVariable UUID ticketId,
                                          @Valid @RequestBody TicketReplyRequestDto request) {

        return supportTicketService.replyAsClient(userId, ticketId, request);
    }
}
