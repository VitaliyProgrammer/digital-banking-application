package io.github.vitaliyprogrammer.banking.account.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record AccountResponseDto(
        UUID id,
        String number,
        Currency currency,
        AccountStatus status,
        BigDecimal balance,
        Instant createdAt
) {

    /** The ledger keeps 4 decimal places for future interest calculations; clients see kopiyky/cents. */
    public static AccountResponseDto from(Account account) {
        return new AccountResponseDto(
                account.getId(),
                account.getNumber(),
                account.getCurrency(),
                account.getStatus(),
                account.getBalance().setScale(2, RoundingMode.HALF_EVEN),
                account.getCreatedAt());
    }
}
