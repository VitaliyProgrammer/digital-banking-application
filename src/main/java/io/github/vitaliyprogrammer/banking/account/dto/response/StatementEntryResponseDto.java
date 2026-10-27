package io.github.vitaliyprogrammer.banking.account.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.LedgerEntry;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** One line of an account statement: what happened, how much, and the balance right after it. */
public record StatementEntryResponseDto(
        UUID transactionId,
        TransactionType type,
        EntryDirection direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt
) {

    public static StatementEntryResponseDto from(LedgerEntry entry) {
        return new StatementEntryResponseDto(
                entry.getTransaction().getId(),
                entry.getTransaction().getType(),
                entry.getDirection(),
                entry.getAmount().setScale(2, RoundingMode.HALF_EVEN),
                entry.getBalanceAfter().setScale(2, RoundingMode.HALF_EVEN),
                entry.getTransaction().getDescription(),
                entry.getCreatedAt());
    }
}
