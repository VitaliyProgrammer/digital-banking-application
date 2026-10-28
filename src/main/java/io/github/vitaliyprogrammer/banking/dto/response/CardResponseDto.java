package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.Card;
import io.github.vitaliyprogrammer.banking.entity.status.CardStatus;
import io.github.vitaliyprogrammer.banking.entity.status.CardType;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** What the app shows in a card list: never the full number, never the CVV. */
public record CardResponseDto(
        UUID id,
        UUID accountId,
        CardType type,
        CardStatus status,
        String maskedNumber,
        String expiry,
        boolean pinSet,
        Instant createdAt
) {

    public static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    public static CardResponseDto from(Card card) {
        return new CardResponseDto(
                card.getId(),
                card.getAccountId(),
                card.getType(),
                card.getStatus(),
                "**** **** **** " + card.getPanLastFour(),
                card.getExpiresOn().format(EXPIRY_FORMAT),
                card.isPinSet(),
                card.getCreatedAt());
    }
}
