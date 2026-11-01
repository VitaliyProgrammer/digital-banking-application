package io.github.vitaliyprogrammer.banking.support.controller;

import io.github.vitaliyprogrammer.banking.common.configuration.OpenApiConfiguration;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.common.security.CurrentUserId;
import io.github.vitaliyprogrammer.banking.support.dto.request.TicketReplyRequestDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.SupportClientViewDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.TicketDetailsResponseDto;
import io.github.vitaliyprogrammer.banking.support.dto.response.TicketResponseDto;
import io.github.vitaliyprogrammer.banking.support.entity.status.TicketStatus;
import io.github.vitaliyprogrammer.banking.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** For SUPPORT and ADMIN (see SecurityConfiguration). */
@RestController
@RequestMapping("/admin/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support (staff)", description = "The support queue: take a ticket, answer, resolve")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get tickets by status", description = "OPEN is the queue, oldest first")
    public PageResponseDto<TicketResponseDto> getTickets(
            @RequestParam(defaultValue = "OPEN") TicketStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return supportTicketService.getTickets(status, page, size);
    }

    @GetMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get a ticket", description = "With the whole conversation")
    public TicketDetailsResponseDto getTicket(@PathVariable UUID ticketId) {

        return supportTicketService.getTicket(ticketId);
    }

    @PostMapping("/{ticketId}/take")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Take a ticket from the queue",
            description = "From now on only you answer it. 409 if someone else was faster")
    public TicketDetailsResponseDto take(@CurrentUserId UUID staffId, @PathVariable UUID ticketId) {

        return supportTicketService.take(staffId, ticketId);
    }

    @PostMapping("/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Answer the client", description = "The client gets a notification and an email")
    public TicketDetailsResponseDto reply(@CurrentUserId UUID staffId,
                                          @PathVariable UUID ticketId,
                                          @Valid @RequestBody TicketReplyRequestDto request) {

        return supportTicketService.replyAsStaff(staffId, ticketId, request);
    }

    @PostMapping("/{ticketId}/resolve")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Resolve a ticket")
    public TicketDetailsResponseDto resolve(@CurrentUserId UUID staffId, @PathVariable UUID ticketId) {

        return supportTicketService.resolve(staffId, ticketId);
    }

    @GetMapping("/{ticketId}/client")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "See the client of my ticket",
            description = "Profile and accounts, read-only; only for the employee who took the ticket")
    public SupportClientViewDto getClientView(@CurrentUserId UUID staffId, @PathVariable UUID ticketId) {

        return supportTicketService.getClientView(staffId, ticketId);
    }
}
