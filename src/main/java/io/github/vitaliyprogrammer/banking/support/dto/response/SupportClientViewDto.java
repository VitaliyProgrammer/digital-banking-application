package io.github.vitaliyprogrammer.banking.support.dto.response;

import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.identity.dto.response.UserResponseDto;
import java.util.List;

/** What the employee handling a ticket may see about its client: the profile and the accounts, read-only. */
public record SupportClientViewDto(
        UserResponseDto client,
        List<AccountResponseDto> accounts
) {
}
