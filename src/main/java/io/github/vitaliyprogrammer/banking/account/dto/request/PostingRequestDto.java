package io.github.vitaliyprogrammer.banking.account.dto.request;

import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Internal command for the ledger, not an HTTP body: other modules (payments, cards) describe
 * a money movement with it. A simple transfer is two lines; a transfer with a fee would be three.
 */
public record PostingRequestDto(
        TransactionType type,
        String description,
        List<Line> lines
) {

    public record Line(UUID accountId, EntryDirection direction, BigDecimal amount) {
    }

    public static PostingRequestDto transfer(TransactionType type,
                                             String description,
                                             UUID fromAccountId,
                                             UUID toAccountId,
                                             BigDecimal amount) {
        return new PostingRequestDto(type, description, List.of(
                new Line(fromAccountId, EntryDirection.DEBIT, amount),
                new Line(toAccountId, EntryDirection.CREDIT, amount)));
    }
}
