package io.github.vitaliyprogrammer.banking.card.dto.request;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IssueCardRequestDto(
        @NotNull
        UUID accountId,

        @NotNull
        CardType type
) {
}
