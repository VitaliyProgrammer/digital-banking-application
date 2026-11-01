package io.github.vitaliyprogrammer.banking.card.dto.response;

import io.github.vitaliyprogrammer.banking.card.entity.status.CardType;
import java.util.UUID;

/** What other modules (payments) need about a card to move money with it. Carries no card secrets. */
public record CardReferenceDto(
        UUID id,
        UUID ownerId,
        UUID accountId,
        CardType type,
        String lastFour,
        boolean usable
) {
}
