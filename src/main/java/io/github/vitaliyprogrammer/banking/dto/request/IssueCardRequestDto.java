package io.github.vitaliyprogrammer.banking.dto.request;

import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IssueCardRequestDto(
        @NotNull
        UUID accountId,

        @NotNull
        CardType type
) {
}
