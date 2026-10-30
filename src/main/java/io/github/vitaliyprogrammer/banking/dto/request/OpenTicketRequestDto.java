package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenTicketRequestDto(
        @NotBlank
        @Size(max = 120)
        String subject,

        /* The first message of the conversation. */
        @NotBlank
        @Size(max = 2000)
        String message
) {
}
