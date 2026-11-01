package io.github.vitaliyprogrammer.banking.support.dto.response;

import java.util.List;

/** A ticket with its whole conversation, oldest message first. */
public record TicketDetailsResponseDto(
        TicketResponseDto ticket,
        List<TicketMessageResponseDto> messages
) {
}
