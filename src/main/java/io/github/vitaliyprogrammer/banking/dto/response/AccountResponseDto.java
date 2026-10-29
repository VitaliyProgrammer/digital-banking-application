package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.Account;
import io.github.vitaliyprogrammer.banking.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public record AccountResponseDto(
        UUID id,
        String number,
        Currency currency,
        /* CLIENT for a current account, SAVINGS for a savings jar. */
        AccountType type,
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
                account.getType(),
                account.getStatus(),
                account.getBalance().setScale(2, RoundingMode.HALF_EVEN),
                account.getCreatedAt());
    }
}
