package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketReplyRequestDto(
        @NotBlank
        @Size(max = 2000)
        String message
) {
}
