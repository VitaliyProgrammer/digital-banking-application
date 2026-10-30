package io.github.vitaliyprogrammer.banking.dto.response;

import java.util.List;

/** What the employee handling a ticket may see about its client: the profile and the accounts, read-only. */
public record SupportClientViewDto(
        UserResponseDto client,
        List<AccountResponseDto> accounts
) {
}
